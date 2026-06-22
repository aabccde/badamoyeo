package badamoyeo_api.ai.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.ai.dto.ChatCompletionRequest;
import badamoyeo_api.ai.dto.ChatCompletionResponse;
import badamoyeo_api.ai.service.ChatbotService;

@RestController
@RequestMapping("/ai/chat-completions")
public class ChatCompletionController {
	private final ChatbotService chatbotService;

	public ChatCompletionController(ChatbotService chatbotService) {
		this.chatbotService = chatbotService;
	}

	@PostMapping
	public ChatCompletionResponse create(@Valid @RequestBody ChatCompletionRequest request) {
		return chatbotService.complete(request.message());
	}
}
