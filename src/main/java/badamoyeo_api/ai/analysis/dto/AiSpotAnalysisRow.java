package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AiSpotAnalysisRow(
	Long spotId,
	Long forecastId,
	String experience,
	String spotName,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	Boolean recommended,
	String recommendationReason,
	LocalDateTime sourceForecastUpdatedAt,
	LocalDateTime generatedAt
) {
}
