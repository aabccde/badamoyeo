package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SpotCardRow(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	Integer postCount,
	Boolean favorite,
	String aiReason,
	Map<String, Object> metrics
) {
	public SpotCardResponse toResponse(java.util.List<SpotForecastResponse> forecasts) {
		return new SpotCardResponse(
			spotId, experience, spotName, lat, lng, region, forecastDate, postCount, favorite, aiReason, forecasts
		);
	}
}
