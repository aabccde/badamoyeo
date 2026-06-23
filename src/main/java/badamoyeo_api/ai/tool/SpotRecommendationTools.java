package badamoyeo_api.ai.tool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.dto.AiSpotSearchCondition;
import badamoyeo_api.ai.dto.AiSpotSearchResult;
import badamoyeo_api.ai.mapper.AiSpotMapper;

@Component
public class SpotRecommendationTools {
	private static final Set<String> EXPERIENCES = Set.of(
		"seaTravel", "swimming", "mudflat", "scuba", "fishing", "surfing"
	);
	private static final Set<String> SORTS = Set.of("best", "worst", "community");

	private final AiSpotMapper aiSpotMapper;

	public SpotRecommendationTools(AiSpotMapper aiSpotMapper) {
		this.aiSpotMapper = aiSpotMapper;
	}

	public List<AiSpotSearchResult> searchMarineSpots(
		String experience,
		String region,
		String keyword,
		String targetDate,
		String sort,
		int limit
	) {
		return aiSpotMapper.searchSpots(new AiSpotSearchCondition(
			normalizeExperience(experience),
			normalize(region),
			normalize(keyword),
			parseTargetDate(targetDate),
			SORTS.contains(sort) ? sort : "best",
			Math.min(Math.max(limit, 1), 10)
		));
	}

	private String normalizeExperience(String experience) {
		String normalized = normalize(experience);
		return EXPERIENCES.contains(normalized) ? normalized : null;
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private LocalDate parseTargetDate(String value) {
		String normalized = normalize(value);
		if (normalized == null) {
			return null;
		}
		try {
			return LocalDate.parse(normalized);
		} catch (DateTimeParseException exception) {
			throw new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"AI generated an invalid targetDate: " + normalized,
				exception
			);
		}
	}
}
