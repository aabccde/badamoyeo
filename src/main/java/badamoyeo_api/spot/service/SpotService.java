package badamoyeo_api.spot.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.common.PageResponse;
import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.spot.dto.Experience;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.dto.SpotDetailResponse;
import badamoyeo_api.spot.dto.SpotSearchCondition;
import badamoyeo_api.spot.mapper.SpotMapper;

@Service
public class SpotService {
	private final SpotMapper spotMapper;

	public SpotService(SpotMapper spotMapper) {
		this.spotMapper = spotMapper;
	}

	public List<MarkerResponse> findMarkers(String experience, LocalDate targetDate) {
		return spotMapper.findMarkers(Experience.from(experience).apiValue(), effectiveTargetDate(targetDate));
	}

	public PageResponse<SpotCardResponse> findSpots(String experience, String sort, LocalDate targetDate, String region,
		String keyword, int page, int pageSize, Double userLat, Double userLng, Long userId) {
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		SpotSearchCondition condition = condition(experience, sort, effectiveTargetDate(targetDate), region, keyword, size, (currentPage - 1) * size, userLat, userLng, userId);
		List<SpotCardResponse> items = spotMapper.findSpotCards(condition);
		long totalCount = spotMapper.countSpotCards(condition);
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	public List<SpotCardResponse> findTopSpots(String experience, String sort, LocalDate targetDate, int limit,
		Double userLat, Double userLng, Long userId) {
		int size = Math.min(Math.max(limit, 1), 100);
		return spotMapper.findSpotCards(condition(experience, sort, effectiveTargetDate(targetDate), null, null, size, 0, userLat, userLng, userId));
	}

	public long countSpots(String experience, LocalDate targetDate) {
		return spotMapper.countSpotCards(condition(experience, "index", effectiveTargetDate(targetDate), null, null, 1, 0, null, null, null));
	}

	public SpotDetailResponse findSpotDetail(Long spotId, LocalDate targetDate, Long userId) {
		SpotDetailResponse detail = spotMapper.findSpotDetail(spotId, effectiveTargetDate(targetDate), userId);
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot not found");
		}
		return detail;
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
		if (!List.of("index", "community", "nearby").contains(sort)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported sort: " + sort);
		}
		return sort;
	}

	private LocalDate effectiveTargetDate(LocalDate targetDate) {
		return targetDate == null ? LocalDate.now() : targetDate;
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
