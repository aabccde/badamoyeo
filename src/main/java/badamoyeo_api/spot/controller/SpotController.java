package badamoyeo_api.spot.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.common.PageResponse;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.dto.SpotDetailResponse;
import badamoyeo_api.spot.service.SpotService;

@RestController
public class SpotController {
	private final SpotService spotService;

	public SpotController(SpotService spotService) {
		this.spotService = spotService;
	}

	@GetMapping("/spots")
	public PageResponse<SpotCardResponse> spots(
		@RequestParam String experience,
		@RequestParam(defaultValue = "index") String sort,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
		@RequestParam(required = false) String region,
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize,
		@RequestParam(required = false) Double userLat,
		@RequestParam(required = false) Double userLng,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return spotService.findSpots(experience, sort, targetDate, region, keyword, page, pageSize, userLat, userLng, userId(authUser));
	}

	@GetMapping("/spots/{spotId}")
	public SpotDetailResponse spotDetail(
		@PathVariable Long spotId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return spotService.findSpotDetail(spotId, targetDate, userId(authUser));
	}

	@PostMapping("/spots/{spotId}/favorite")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addFavorite(@PathVariable Long spotId, @AuthenticationPrincipal AuthUser authUser) {
		spotService.addFavorite(spotId, userId(authUser));
	}

	@DeleteMapping("/spots/{spotId}/favorite")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeFavorite(@PathVariable Long spotId, @AuthenticationPrincipal AuthUser authUser) {
		spotService.removeFavorite(spotId, userId(authUser));
	}

	private Long userId(AuthUser authUser) {
		return authUser == null ? null : authUser.userId();
	}
}
