package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDateTime;

public record AiSpotAnalysisSaveRequest(
	Long spotId,
	Long forecastId,
	boolean recommended,
	String recommendationReason,
	LocalDateTime sourceForecastUpdatedAt,
	LocalDateTime generatedAt
) {
}
