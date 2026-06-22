package badamoyeo_api.ai.recommendation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record AiSpotRecommendationResponse(
	Long spotId,
	String experience,
	String spotName,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	Integer postCount,
	Boolean favorite,
	Map<String, Object> metrics,
	int rank,
	String reason,
	LocalDateTime generatedAt
) {
}
