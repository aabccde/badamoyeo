package badamoyeo_api.ai.recommendation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationItem;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationSaveRequest;
import badamoyeo_api.ai.recommendation.dto.AiSpotRecommendationResponse;
import badamoyeo_api.ai.recommendation.dto.AiSpotRecommendationsResponse;
import badamoyeo_api.ai.recommendation.mapper.AiRecommendationMapper;
import badamoyeo_api.spot.dto.Experience;

@Service
public class AiRecommendationService {
	private static final int CANDIDATE_LIMIT = 20;
	private static final int RECOMMENDATION_LIMIT = 6;

	private final AiRecommendationMapper recommendationMapper;
	private final AiRecommendationGenerator recommendationGenerator;
	private final TransactionTemplate transactionTemplate;

	public AiRecommendationService(
		AiRecommendationMapper recommendationMapper,
		AiRecommendationGenerator recommendationGenerator,
		TransactionTemplate transactionTemplate
	) {
		this.recommendationMapper = recommendationMapper;
		this.recommendationGenerator = recommendationGenerator;
		this.transactionTemplate = transactionTemplate;
	}

	public void refresh(String experience, LocalDate forecastDate) {
		refresh(experience, forecastDate, false);
	}

	public void refreshIfAbsent(String experience, LocalDate forecastDate) {
		refresh(experience, forecastDate, true);
	}

	private void refresh(String experience, LocalDate forecastDate, boolean onlyIfAbsent) {
		String normalizedExperience = Experience.from(experience).apiValue();
		LocalDate recommendationDate = recommendationMapper.findRecommendationForecastDate(
			normalizedExperience,
			forecastDate
		);
		if (recommendationDate == null) {
			return;
		}
		if (onlyIfAbsent && recommendationMapper.existsRecommendations(normalizedExperience, recommendationDate)) {
			return;
		}
		List<AiRecommendationCandidate> candidates = recommendationMapper.findCandidates(
			normalizedExperience,
			recommendationDate,
			CANDIDATE_LIMIT
		);
		if (candidates.isEmpty()) {
			return;
		}

		List<AiRecommendationItem> generated = recommendationGenerator.generate(normalizedExperience, candidates);
		List<AiRecommendationSaveRequest> recommendations = validateAndConvert(
			normalizedExperience,
			recommendationDate,
			candidates,
			generated
		);

		transactionTemplate.executeWithoutResult(status -> {
			recommendationMapper.deleteRecommendations(normalizedExperience, recommendationDate);
			recommendationMapper.insertRecommendations(recommendations);
		});
	}

	public AiSpotRecommendationsResponse findLatest(String experience, Long userId) {
		String normalizedExperience = Experience.from(experience).apiValue();
		List<AiSpotRecommendationResponse> items =
			recommendationMapper.findLatestRecommendations(normalizedExperience, userId);
		if (items.isEmpty()) {
			return new AiSpotRecommendationsResponse(normalizedExperience, null, null, List.of());
		}
		return new AiSpotRecommendationsResponse(
			normalizedExperience,
			items.getFirst().forecastDate(),
			items.getFirst().generatedAt(),
			items
		);
	}

	private List<AiRecommendationSaveRequest> validateAndConvert(
		String experience,
		LocalDate forecastDate,
		List<AiRecommendationCandidate> candidates,
		List<AiRecommendationItem> generated
	) {
		int requiredCount = Math.min(RECOMMENDATION_LIMIT, candidates.size());
		if (generated == null || generated.size() != requiredCount) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI must return " + requiredCount + " recommendations");
		}

		Map<Long, AiRecommendationCandidate> candidatesById = candidates.stream()
			.collect(Collectors.toMap(AiRecommendationCandidate::spotId, Function.identity()));
		Set<Long> spotIds = new HashSet<>();
		Set<Integer> ranks = new HashSet<>();
		LocalDateTime generatedAt = LocalDateTime.now();

		return generated.stream()
			.peek(item -> validateItem(item, requiredCount, candidatesById, spotIds, ranks))
			.sorted(java.util.Comparator.comparingInt(AiRecommendationItem::rank))
			.map(item -> new AiRecommendationSaveRequest(
				experience,
				forecastDate,
				item.spotId(),
				item.rank(),
				item.reason().trim(),
				generatedAt
			))
			.toList();
	}

	private void validateItem(
		AiRecommendationItem item,
		int requiredCount,
		Map<Long, AiRecommendationCandidate> candidatesById,
		Set<Long> spotIds,
		Set<Integer> ranks
	) {
		if (item == null || item.spotId() == null || !candidatesById.containsKey(item.spotId())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an unknown spot");
		}
		if (!spotIds.add(item.spotId())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned a duplicate spot");
		}
		if (item.rank() < 1 || item.rank() > requiredCount || !ranks.add(item.rank())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an invalid rank");
		}
		if (item.reason() == null || item.reason().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an empty recommendation reason");
		}
		if (item.reason().length() > 500) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI recommendation reason is too long");
		}
	}
}
