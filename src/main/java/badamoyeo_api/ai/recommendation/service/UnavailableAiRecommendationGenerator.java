package badamoyeo_api.ai.recommendation.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationItem;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
public class UnavailableAiRecommendationGenerator implements AiRecommendationGenerator {
	@Override
	public List<AiRecommendationItem> generate(String experience, List<AiRecommendationCandidate> candidates) {
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI recommendation is not configured");
	}
}
