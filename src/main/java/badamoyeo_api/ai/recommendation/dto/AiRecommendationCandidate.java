package badamoyeo_api.ai.recommendation.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

public record AiRecommendationCandidate(
	Long spotId,
	String spotName,
	String region,
	LocalDate forecastDate,
	Integer postCount,
	JsonNode forecasts
) {
}
