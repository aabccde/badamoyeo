package badamoyeo_api.spot.dto;

import java.util.Map;

public record SpotForecastResponse(
	Long forecastId,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Map<String, Object> metrics,
	SpotForecastAiAnalysisResponse aiAnalysis
) {
}
