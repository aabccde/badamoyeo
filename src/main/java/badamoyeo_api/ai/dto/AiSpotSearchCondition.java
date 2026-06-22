package badamoyeo_api.ai.dto;

public record AiSpotSearchCondition(
	String experience,
	String region,
	String keyword,
	String sort,
	int limit
) {
}
