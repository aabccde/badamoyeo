package badamoyeo_api.ai.recommendation.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationItem;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationSelection;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
public class SpringAiRecommendationGenerator implements AiRecommendationGenerator {
	private static final String SYSTEM_PROMPT = """
		당신은 바다모여의 해양 레저 장소 추천 분석가입니다.
		제공된 DB 후보만 평가하고 새로운 장소나 수치를 만들지 마세요.
		종합지수, 날씨, 물때, 체험별 세부 지표, 게시글 수를 함께 고려하세요.
		안전이 보장된다고 단정하지 말고 데이터에 근거한 간결한 한국어 추천 이유를 작성하세요.
		반환하는 spotId는 후보에 있는 값만 사용하고 중복하지 마세요.
		""";

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;

	public SpringAiRecommendationGenerator(ChatClient.Builder builder, ObjectMapper objectMapper) {
		this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
		this.objectMapper = objectMapper;
	}

	@Override
	public List<AiRecommendationItem> generate(String experience, List<AiRecommendationCandidate> candidates) {
		int count = Math.min(6, candidates.size());
		try {
			AiRecommendationSelection selection = chatClient.prompt()
				.user("""
					체험 종류: %s
					아래 후보 중 추천 장소를 정확히 %d곳 선정하세요.
					rank는 1부터 시작해 중복 없이 부여하고, reason은 100자 이내로 작성하세요.

					후보 데이터:
					%s
					""".formatted(experience, count, objectMapper.writeValueAsString(candidates)))
				.call()
				.entity(AiRecommendationSelection.class);
			if (selection == null || selection.recommendations() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI recommendation response is empty");
			}
			return selection.recommendations();
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"failed to serialize recommendation candidates",
				exception
			);
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI recommendation request failed", exception);
		}
	}
}
