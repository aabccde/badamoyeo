package badamoyeo_api.ai.recommendation.dto;

import java.time.LocalDate;
import java.util.Map;

public record AiRecommendationCandidate(
	Long spotId,
	String spotName,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Integer postCount,
	Map<String, Object> metrics
) {
}
