package badamoyeo_api.ai.dto;

import java.time.Instant;

public record ChatCompletionResponse(
	String content,
	Instant createdAt
) {
}
