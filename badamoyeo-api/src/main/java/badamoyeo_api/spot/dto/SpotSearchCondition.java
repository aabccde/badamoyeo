package badamoyeo_api.spot.dto;

import java.time.LocalDate;

public record SpotSearchCondition(
	String experience,
	String sort,
	LocalDate targetDate,
	String region,
	String keyword,
	Double userLat,
	Double userLng,
	Long userId,
	int limit,
	int offset
) {
}
