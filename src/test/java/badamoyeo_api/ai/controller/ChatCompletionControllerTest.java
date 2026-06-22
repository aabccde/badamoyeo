package badamoyeo_api.ai.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import badamoyeo_api.ai.dto.ChatCompletionResponse;
import badamoyeo_api.ai.service.ChatbotService;
import badamoyeo_api.auth.security.JwtAuthenticationFilter;
import badamoyeo_api.common.ApiExceptionHandler;

@WebMvcTest(ChatCompletionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class ChatCompletionControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatbotService chatbotService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Test
	void createsChatCompletion() throws Exception {
		Instant createdAt = Instant.parse("2026-06-22T03:00:00Z");
		when(chatbotService.complete("서핑 조건을 알려줘"))
			.thenReturn(new ChatCompletionResponse("파고와 풍속을 확인하세요.", createdAt));

		mockMvc.perform(post("/ai/chat-completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"message":"서핑 조건을 알려줘"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value("파고와 풍속을 확인하세요."))
			.andExpect(jsonPath("$.createdAt").value("2026-06-22T03:00:00Z"));

		verify(chatbotService).complete("서핑 조건을 알려줘");
	}

	@Test
	void rejectsBlankMessage() throws Exception {
		mockMvc.perform(post("/ai/chat-completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"message":" "}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("message: message is required"));
	}

	@Test
	void rejectsMessageLongerThanLimit() throws Exception {
		String message = "a".repeat(2001);

		mockMvc.perform(post("/ai/chat-completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"" + message + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("message: message must be 2000 characters or less"));
	}
}
