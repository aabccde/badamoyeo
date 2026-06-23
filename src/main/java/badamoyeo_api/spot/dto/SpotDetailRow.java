package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SpotDetailRow(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Boolean favorite,
	Map<String, Object> metrics
) {
	public SpotDetailResponse toResponse(java.util.List<SpotForecastResponse> forecasts) {
		return new SpotDetailResponse(
			spotId, experience, spotName, lat, lng, region, forecastDate, favorite, forecasts
		);
	}
}
