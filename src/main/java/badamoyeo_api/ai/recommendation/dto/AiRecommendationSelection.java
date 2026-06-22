package badamoyeo_api.ai.recommendation.dto;

import java.util.List;

public record AiRecommendationSelection(
	List<AiRecommendationItem> recommendations
) {
}
