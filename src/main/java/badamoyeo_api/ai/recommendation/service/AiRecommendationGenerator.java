package badamoyeo_api.ai.recommendation.service;

import java.util.List;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationItem;

public interface AiRecommendationGenerator {
	List<AiRecommendationItem> generate(String experience, List<AiRecommendationCandidate> candidates);
}
