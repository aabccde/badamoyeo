package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiSpotAnalysisResponse(
	Long spotId,
	Long forecastId,
	String experience,
	String spotName,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String summary,
	List<String> advantages,
	List<String> disadvantages,
	boolean recommended,
	String recommendationReason,
	String safetyNote,
	LocalDateTime generatedAt
) {
}
