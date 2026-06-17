package badamoyeo_api.dashboard.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import badamoyeo_api.dashboard.dto.DashboardResponse;
import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.service.SpotService;

@Service
public class DashboardService {
	private final SpotService spotService;

	public DashboardService(SpotService spotService) {
		this.spotService = spotService;
	}

	public List<MarkerResponse> findMarkers(String experience, LocalDate targetDate) {
		return spotService.findMarkers(experience, targetDate);
	}

	public DashboardResponse findDashboard(String experience, int limit, String sort, LocalDate targetDate,
		Double userLat, Double userLng, Long userId) {
		List<SpotCardResponse> items = spotService.findTopSpots(experience, sort, targetDate, limit, userLat, userLng, userId);
		long totalCount = spotService.countSpots(experience, targetDate);
		return new DashboardResponse(totalCount, items);
	}
}
