package badamoyeo_api.spot.dto;

import java.time.LocalDate;
import java.util.Map;

public record SpotForecastRow(
	Long spotId,
	Long forecastId,
	LocalDate forecastDate,
	String timeSlot,
	String variantKey,
	String totalIndex,
	String weather,
	String tide,
	Map<String, Object> metrics
) {
	public SpotForecastResponse toResponse() {
		return new SpotForecastResponse(
			forecastId, timeSlot, totalIndex, weather, tide, metrics
		);
	}
}
