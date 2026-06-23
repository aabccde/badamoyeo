package badamoyeo_api.ai.dto;

import java.time.LocalDate;

public record AiSpotSearchCondition(
	String experience,
	String region,
	String keyword,
	LocalDate targetDate,
	String sort,
	int limit
) {
}
