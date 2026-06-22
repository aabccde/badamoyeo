package badamoyeo_api.ai.recommendation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AiRecommendationSaveRequest(
	String experience,
	LocalDate forecastDate,
	Long spotId,
	int rank,
	String reason,
	LocalDateTime generatedAt
) {
}
