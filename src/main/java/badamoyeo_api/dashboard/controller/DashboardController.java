package badamoyeo_api.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.dashboard.dto.DashboardResponse;
import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.dashboard.service.DashboardService;

@RestController
public class DashboardController {
	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/dashboard/markers")
	public List<MarkerResponse> markers(
		@RequestParam String experience,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
	) {
		return dashboardService.findMarkers(experience, targetDate);
	}

	@GetMapping("/dashboard")
	public DashboardResponse dashboard(
		@RequestParam String experience,
		@RequestParam(defaultValue = "6") int limit,
		@RequestParam(defaultValue = "index") String sort,
		@RequestParam(required = false) Double userLng,
		@RequestParam(required = false) Double userLat,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return dashboardService.findDashboard(experience, limit, sort, targetDate, userLat, userLng, userId(authUser));
	}

	private Long userId(AuthUser authUser) {
		return authUser == null ? null : authUser.userId();
	}
}
