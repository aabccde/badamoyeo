package badamoyeo_api.ai.recommendation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiSpotRecommendationsResponse(
	String experience,
	LocalDate forecastDate,
	LocalDateTime generatedAt,
	List<AiSpotRecommendationResponse> items
) {
}
