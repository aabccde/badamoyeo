package badamoyeo_api.ai.analysis.service;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;

public interface AiSpotAnalysisGenerator {
	AiSpotAnalysisContent generate(AiSpotAnalysisSource source);
}
