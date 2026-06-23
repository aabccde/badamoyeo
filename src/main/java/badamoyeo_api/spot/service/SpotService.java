package badamoyeo_api.spot.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.service.AiSpotAnalysisService;
import badamoyeo_api.common.PageResponse;
import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.spot.dto.Experience;
import badamoyeo_api.spot.dto.ForecastTimeSlot;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.dto.SpotCardRow;
import badamoyeo_api.spot.dto.SpotDetailResponse;
import badamoyeo_api.spot.dto.SpotDetailRow;
import badamoyeo_api.spot.dto.SpotForecastResponse;
import badamoyeo_api.spot.dto.SpotForecastAiAnalysisResponse;
import badamoyeo_api.spot.dto.SpotForecastRow;
import badamoyeo_api.spot.dto.SpotSearchCondition;
import badamoyeo_api.spot.mapper.SpotMapper;

@Service
public class SpotService {
	private final SpotMapper spotMapper;
	private final AiSpotAnalysisService analysisService;

	public SpotService(SpotMapper spotMapper, AiSpotAnalysisService analysisService) {
		this.spotMapper = spotMapper;
		this.analysisService = analysisService;
	}

	public List<MarkerResponse> findMarkers(String experience, LocalDate targetDate, String timeSlot) {
		return spotMapper.findMarkers(
			Experience.from(experience).apiValue(),
			effectiveTargetDate(targetDate),
			ForecastTimeSlot.normalize(timeSlot)
		);
	}

	public PageResponse<SpotCardResponse> findSpots(String experience, String sort, LocalDate targetDate, String region,
		String keyword, int page, int pageSize, Double userLat, Double userLng, Long userId) {
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		LocalDate date = effectiveTargetDate(targetDate);
		SpotSearchCondition condition = condition(experience, sort, date, region, keyword, size, (currentPage - 1) * size, userLat, userLng, userId);
		List<SpotCardResponse> items = attachForecasts(spotMapper.findSpotCards(condition), date);
		long totalCount = spotMapper.countSpotCards(condition);
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	public List<SpotCardResponse> findTopSpots(String experience, String sort, LocalDate targetDate, int limit,
		Double userLat, Double userLng, Long userId) {
		int size = Math.min(Math.max(limit, 1), 100);
		LocalDate date = effectiveTargetDate(targetDate);
		return attachForecasts(
			spotMapper.findSpotCards(condition(experience, sort, date, null, null, size, 0, userLat, userLng, userId)),
			date
		);
	}

	public long countSpots(String experience, LocalDate targetDate) {
		return spotMapper.countSpotCards(condition(experience, "index", effectiveTargetDate(targetDate), null, null, 1, 0, null, null, null));
	}

	public SpotDetailResponse findSpotDetail(Long spotId, LocalDate targetDate, Long userId) {
		LocalDate date = effectiveTargetDate(targetDate);
		SpotDetailRow detail = spotMapper.findSpotDetail(spotId, date, userId);
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot not found");
		}
		List<SpotForecastResponse> forecasts = spotMapper.findSpotForecasts(List.of(spotId), date, null)
			.parallelStream()
			.map(row -> row.toResponse(toSpotAnalysis(analysisService.findOrCreateByForecastId(row.forecastId()))))
			.toList();
		return detail.toResponse(forecasts);
	}

	public List<SpotCardResponse> attachForecasts(List<SpotCardRow> rows, LocalDate targetDate) {
		return attachForecasts(rows, targetDate, null);
	}

	public List<SpotCardResponse> attachForecasts(
		List<SpotCardRow> rows,
		LocalDate targetDate,
		String timeSlot
	) {
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> spotIds = rows.stream().map(SpotCardRow::spotId).toList();
		Map<Long, List<SpotForecastRow>> forecastRowsBySpot = spotMapper.findSpotForecasts(
				spotIds,
				targetDate,
				ForecastTimeSlot.normalize(timeSlot)
			)
			.stream()
			.collect(Collectors.groupingBy(SpotForecastRow::spotId));
		return rows.stream()
			.map(row -> row.toResponse(
				forecastRowsBySpot.getOrDefault(row.spotId(), List.of())
					.stream()
					.map(SpotForecastRow::toResponse)
					.toList()
			))
			.toList();
	}

	@Transactional
	public void addFavorite(Long spotId, Long userId) {
		requireUser(userId);
		requireActiveSpot(spotId);
		spotMapper.insertFavorite(spotId, userId);
	}

	@Transactional
	public void removeFavorite(Long spotId, Long userId) {
		requireUser(userId);
		requireActiveSpot(spotId);
		spotMapper.deleteFavorite(spotId, userId);
	}

	private SpotSearchCondition condition(String experience, String sort, LocalDate targetDate, String region,
		String keyword, int limit, int offset, Double userLat, Double userLng, Long userId) {
		return new SpotSearchCondition(
			Experience.from(experience).apiValue(),
			normalizeSort(sort),
			targetDate,
			normalizeRegion(region),
			keyword,
			userLat,
			userLng,
			userId,
			limit,
			offset
		);
	}

	private String normalizeRegion(String region) {
		if (region == null || region.isBlank() || "전체".equals(region)) {
			return null;
		}
		return region.trim();
	}

	private String normalizeSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return "index";
		}
		if (!List.of("index", "community", "nearby", "ai").contains(sort)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported sort: " + sort);
		}
		return sort;
	}

	private LocalDate effectiveTargetDate(LocalDate targetDate) {
		return targetDate == null ? LocalDate.now() : targetDate;
	}

	private SpotForecastAiAnalysisResponse toSpotAnalysis(AiSpotAnalysisResponse analysis) {
		return new SpotForecastAiAnalysisResponse(
			analysis.recommended(),
			analysis.recommendationReason()
		);
	}

	private void requireUser(Long userId) {
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization required");
		}
	}

	private void requireActiveSpot(Long spotId) {
		if (!spotMapper.existsActiveSpot(spotId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot not found");
		}
	}
}
