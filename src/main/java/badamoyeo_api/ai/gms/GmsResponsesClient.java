package badamoyeo_api.ai.gms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GmsResponsesClient {
	private static final Logger log = LoggerFactory.getLogger(GmsResponsesClient.class);

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;

	public GmsResponsesClient(
		ObjectMapper objectMapper,
		@Value("${app.ai.gms.responses-url:https://gms.ssafy.io/gmsapi/api.openai.com/v1/responses}")
		String responsesUrl,
		@Value("${app.ai.gms.api-key:}") String apiKey
	) {
		this.restClient = RestClient.builder().baseUrl(responsesUrl).build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
	}

	public String complete(String model, String instructions, String input, int maxOutputTokens) {
		Map<String, Object> request = baseRequest(model, instructions, input, maxOutputTokens);
		return outputText(execute(request));
	}

	public <T> T completeJson(
		String model,
		String instructions,
		String input,
		int maxOutputTokens,
		Class<T> responseType
	) {
		Map<String, Object> request = baseRequest(
			model,
			instructions,
			"반드시 요청된 구조의 유효한 JSON 객체만 반환하세요.\n\n" + input,
			maxOutputTokens
		);
		request.put("text", Map.of("format", Map.of("type", "json_object")));
		try {
			return objectMapper.readValue(outputText(execute(request)), responseType);
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to parse GMS response", exception);
		}
	}

	public JsonNode execute(Map<String, Object> request) {
		requireApiKey();
		String requestedModel = String.valueOf(request.get("model"));
		try {
			log.info("Requesting GMS response. model={}", requestedModel);
			String responseBody = restClient.post()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(String.class);
			if (responseBody == null || responseBody.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GMS returned an empty response");
			}
			JsonNode response = objectMapper.readTree(responseBody);
			log.info(
				"Received GMS response. requestedModel={}, responseModel={}, responseId={}, status={}, inputTokens={}, outputTokens={}",
				requestedModel,
				response.path("model").asText("unknown"),
				response.path("id").asText("unknown"),
				response.path("status").asText("unknown"),
				response.path("usage").path("input_tokens").asInt(0),
				response.path("usage").path("output_tokens").asInt(0)
			);
			return response;
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (RestClientResponseException exception) {
			throw new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"GMS request failed: HTTP %d %s".formatted(
					exception.getStatusCode().value(),
					exception.getResponseBodyAsString()
				),
				exception
			);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GMS request failed", exception);
		}
	}

	public String outputText(JsonNode response) {
		List<String> texts = new ArrayList<>();
		for (JsonNode output : response.path("output")) {
			if (!"message".equals(output.path("type").asText())) {
				continue;
			}
			for (JsonNode content : output.path("content")) {
				if ("output_text".equals(content.path("type").asText())) {
					String text = content.path("text").asText();
					if (!text.isBlank()) {
						texts.add(text);
					}
				}
			}
		}
		if (texts.isEmpty()) {
			String status = response.path("status").asText("unknown");
			String incompleteReason = response.path("incomplete_details").path("reason").asText("");
			String refusal = findRefusal(response);
			String detail = !incompleteReason.isBlank()
				? "status=%s, reason=%s".formatted(status, incompleteReason)
				: !refusal.isBlank()
					? "status=%s, refusal=%s".formatted(status, refusal)
					: "status=" + status;
			throw new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"GMS response has no output text (" + detail + ")"
			);
		}
		return String.join("\n", texts);
	}

	private String findRefusal(JsonNode response) {
		for (JsonNode output : response.path("output")) {
			for (JsonNode content : output.path("content")) {
				if ("refusal".equals(content.path("type").asText())) {
					return content.path("refusal").asText("");
				}
			}
		}
		return "";
	}

	public Map<String, Object> baseRequest(
		String model,
		String instructions,
		Object input,
		int maxOutputTokens
	) {
		Map<String, Object> request = new LinkedHashMap<>();
		request.put("model", model);
		request.put("instructions", instructions);
		request.put("input", input);
		request.put("max_output_tokens", maxOutputTokens);
		request.put("reasoning", Map.of("effort", "low"));
		return request;
	}

	private void requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GMS API key is not configured");
		}
	}
}
