package badamoyeo_api.ai.analysis.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
public class UnavailableAiSpotAnalysisGenerator implements AiSpotAnalysisGenerator {
	@Override
	public AiSpotAnalysisContent generate(AiSpotAnalysisSource source) {
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI spot analysis is not configured");
	}
}
