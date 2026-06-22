package badamoyeo_api.ai.analysis.dto;

import java.util.List;

public record AiSpotAnalysisContent(
	String summary,
	List<String> advantages,
	List<String> disadvantages,
	boolean recommended,
	String recommendationReason,
	String safetyNote
) {
}
