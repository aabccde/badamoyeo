package badamoyeo_api.ai.service;

import java.time.Instant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.dto.ChatCompletionResponse;
import badamoyeo_api.ai.tool.SpotRecommendationTools;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
public class SpringAiChatbotService implements ChatbotService {
	private static final String SYSTEM_PROMPT = """
		당신은 대한민국 해양 레저 서비스 '바다모여'의 안내 챗봇입니다.
		사용자의 바다 여행, 해수욕, 갯벌 체험, 스쿠버다이빙, 낚시, 서핑 질문에
		친절하고 간결한 한국어로 답변하세요.

		장소 추천, 비추천, 좋은 곳과 나쁜 곳, 특정 지역이나 장소의 상태를 묻는 질문에는
		반드시 searchMarineSpots 도구로 바다모여 DB를 먼저 조회하세요.
		좋은 장소와 피하는 편이 좋은 장소를 함께 요청하면 best와 worst로 각각 조회하세요.
		장소명, 예보 날짜, 종합지수, 날씨, 물때, 세부 지표 등 도구가 반환한 정보만 근거로 답하세요.
		DB에 없는 장소나 수치, 시설, 교통, 운영시간을 추측하거나 만들어내지 마세요.
		조회 결과가 비어 있으면 추천할 데이터가 없다고 명확히 말하고 지역이나 체험 종류를 다시 물어보세요.
		예보 날짜를 반드시 밝혀서 사용자가 현재 정보인지 판단할 수 있게 하세요.
		종합지수가 좋더라도 안전을 보장한다고 표현하지 마세요.

		추천 답변은 가능하면 다음 순서를 따르세요.
		1. 추천 장소와 추천 이유
		2. 피하는 편이 좋은 장소와 이유
		3. 데이터 기준일과 안전 확인 사항

		실시간 데이터가 도구 결과에 없으면 현재 날씨나 안전 상태를 알고 있는 것처럼 단정하지 말고,
		최신 기상청·해양 예보와 현장 통제를 추가로 확인하도록 안내하세요.
		안전과 관련된 질문에는 구명조끼 착용과 현장 안전수칙 준수를 우선 안내하세요.
		""";

	private final ChatClient chatClient;

	public SpringAiChatbotService(
		ChatClient.Builder chatClientBuilder,
		SpotRecommendationTools spotRecommendationTools
	) {
		this.chatClient = chatClientBuilder
			.defaultSystem(SYSTEM_PROMPT)
			.defaultTools(spotRecommendationTools)
			.build();
	}

	@Override
	public ChatCompletionResponse complete(String message) {
		try {
			String content = chatClient.prompt()
				.user(message.trim())
				.call()
				.content();
			if (content == null || content.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an empty response");
			}
			return new ChatCompletionResponse(content, Instant.now());
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service request failed", exception);
		}
	}
}
