package badamoyeo_api.ai.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.dto.ChatCompletionResponse;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
public class UnavailableChatbotService implements ChatbotService {
	@Override
	public ChatCompletionResponse complete(String message) {
		throw new ResponseStatusException(
			HttpStatus.SERVICE_UNAVAILABLE,
			"AI chatbot is not configured"
		);
	}
}
