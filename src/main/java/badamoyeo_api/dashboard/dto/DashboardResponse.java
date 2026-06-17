package badamoyeo_api.dashboard.dto;

import java.util.List;

import badamoyeo_api.spot.dto.SpotCardResponse;

public record DashboardResponse(
	long totalCount,
	List<SpotCardResponse> items
) {
}
