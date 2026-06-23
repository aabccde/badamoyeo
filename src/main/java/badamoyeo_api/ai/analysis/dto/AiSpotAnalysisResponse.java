package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AiSpotAnalysisResponse(
	Long spotId,
	Long forecastId,
	String experience,
	String spotName,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	boolean recommended,
	String recommendationReason,
	LocalDateTime generatedAt
) {
}
