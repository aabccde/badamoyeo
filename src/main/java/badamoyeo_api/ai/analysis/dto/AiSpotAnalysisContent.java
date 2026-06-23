package badamoyeo_api.ai.analysis.dto;

public record AiSpotAnalysisContent(
	boolean recommended,
	String recommendationReason
) {
}
