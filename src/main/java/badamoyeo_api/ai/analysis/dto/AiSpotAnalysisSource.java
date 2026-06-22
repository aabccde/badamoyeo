package badamoyeo_api.ai.analysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record AiSpotAnalysisSource(
	Long spotId,
	Long forecastId,
	String experience,
	String spotName,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Map<String, Object> metrics,
	LocalDateTime forecastUpdatedAt
) {
}
