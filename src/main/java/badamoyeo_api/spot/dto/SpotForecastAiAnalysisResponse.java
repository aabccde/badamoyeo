package badamoyeo_api.spot.dto;

public record SpotForecastAiAnalysisResponse(
	boolean recommended,
	String recommendationReason
) {
}
