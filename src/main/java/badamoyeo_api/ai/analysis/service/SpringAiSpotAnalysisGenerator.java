package badamoyeo_api.ai.analysis.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
public class SpringAiSpotAnalysisGenerator implements AiSpotAnalysisGenerator {
	private static final String SYSTEM_PROMPT = """
		당신은 바다모여의 해양 레저 장소 분석가입니다.
		제공된 DB 데이터만 사용하고 시설, 교통, 운영시간 또는 수치를 추측하지 마세요.
		종합지수와 체험별 지표를 함께 분석하되 안전을 보장한다고 표현하지 마세요.
		장점과 단점은 각각 1~2개, 각 항목은 80자 이내로 작성하세요.
		요약과 추천 근거는 각각 200자 이내, 안전 안내는 150자 이내로 작성하세요.
		데이터가 부족하면 그 사실을 단점과 안전 안내에 명확히 포함하세요.
		""";

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final String model;

	public SpringAiSpotAnalysisGenerator(
		ChatClient.Builder builder,
		ObjectMapper objectMapper,
		@Value("${app.ai.analysis.model:gpt-5-mini}") String model
	) {
		this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
		this.objectMapper = objectMapper;
		this.model = model;
	}

	@Override
	public AiSpotAnalysisContent generate(AiSpotAnalysisSource source) {
		try {
			AiSpotAnalysisContent content = chatClient.prompt()
				.options(OpenAiChatOptions.builder()
					.model(model)
					.maxCompletionTokens(500))
				.user("""
					다음 장소의 해당 예보를 분석하세요.
					recommended는 이 체험을 위해 방문을 추천할 수 있는 데이터인지 판단한 값입니다.

					%s
					""".formatted(objectMapper.writeValueAsString(source)))
				.call()
				.entity(AiSpotAnalysisContent.class);
			if (content == null) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI analysis response is empty");
			}
			return content;
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize analysis source", exception);
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI analysis request failed", exception);
		}
	}
}
