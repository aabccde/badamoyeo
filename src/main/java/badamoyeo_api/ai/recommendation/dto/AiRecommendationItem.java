package badamoyeo_api.ai.recommendation.dto;

public record AiRecommendationItem(
	Long spotId,
	int rank,
	String reason
) {
}
