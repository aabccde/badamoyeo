package badamoyeo_api.ai.service;

import badamoyeo_api.ai.dto.ChatCompletionResponse;

public interface ChatbotService {
	ChatCompletionResponse complete(String message);
}
