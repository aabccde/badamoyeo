package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDateTime;

public record AiSpotAnalysisSaveRequest(
	Long spotId,
	Long forecastId,
	String summary,
	String advantagesJson,
	String disadvantagesJson,
	boolean recommended,
	String recommendationReason,
	String safetyNote,
	LocalDateTime sourceForecastUpdatedAt,
	LocalDateTime generatedAt
) {
}
