package badamoyeo_api.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.gms.GmsResponsesClient;
import badamoyeo_api.ai.dto.AiSpotSearchResult;
import badamoyeo_api.ai.tool.SpotRecommendationTools;

class GmsAiChatbotServiceTest {
	private final GmsResponsesClient responsesClient = mock(GmsResponsesClient.class);
	private final SpotRecommendationTools spotRecommendationTools = mock(SpotRecommendationTools.class);
	private final GmsAiChatbotService service = new GmsAiChatbotService(
		responsesClient,
		spotRecommendationTools,
		new ObjectMapper(),
		"gpt-5-mini"
	);

	@Test
	void rejectsOutOfScopeQuestionBeforeAiRequest() {
		String content = service.complete("C언어 포인터 알려줘").content();

		assertThat(content).isEqualTo("바다모여에서는 바다 여행, 해수욕, 갯벌 체험, 스쿠버다이빙, 낚시, 서핑 관련 질문만 답변할 수 있어요.");
		verify(responsesClient, never()).execute(any());
	}

	@Test
	void allowsShortMarineLocationQuestion() throws Exception {
		JsonNode response = new ObjectMapper().readTree("{\"output\":[]}");
		when(responsesClient.execute(any())).thenReturn(response);
		when(responsesClient.outputText(response)).thenReturn("신안북동 예보를 확인했어요.");

		assertThat(service.complete("신안 북동 어때?").content()).isEqualTo("신안북동 예보를 확인했어요.");
	}

	@Test
	void hidesToolInternalsFromChatbotResponse() throws Exception {
		JsonNode response = new ObjectMapper().readTree("{\"output\":[]}");
		when(responsesClient.execute(any())).thenReturn(response);
		when(responsesClient.outputText(response))
			.thenReturn("{\"experience\":\"surfing\",\"targetDate\":\"2026-06-29\"}");

		assertThat(service.complete("제주 서핑 장소를 추천해줘").content())
			.isEqualTo("예보 답변을 정리하는 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
	}

	@Test
	void limitsResponseToChatBubbleLength() throws Exception {
		JsonNode response = new ObjectMapper().readTree("{\"output\":[]}");
		when(responsesClient.execute(any())).thenReturn(response);
		when(responsesClient.outputText(response)).thenReturn("가".repeat(600));

		assertThat(service.complete("서핑 준비물을 알려줘").content()).hasSize(500);
	}

	@Test
	void formatsToolResultWithoutSecondAiRequest() throws Exception {
		JsonNode response = new ObjectMapper().readTree("""
			{
			  "output": [
			    {
			      "type": "function_call",
			      "name": "searchMarineSpots",
			      "call_id": "call_1",
			      "arguments": "{\\"experience\\":\\"surfing\\",\\"region\\":\\"제주\\",\\"keyword\\":\\"\\",\\"targetDate\\":\\"2026-06-29\\",\\"sort\\":\\"best\\",\\"limit\\":2}"
			    }
			  ]
			}
			""");
		when(responsesClient.execute(any())).thenReturn(response);
		when(spotRecommendationTools.searchMarineSpots("surfing", "제주", "", "2026-06-29", "best", 2))
			.thenReturn(List.of(new AiSpotSearchResult(
				1L,
				"중문색달해수욕장",
				"surfing",
				"제주",
				LocalDate.of(2026, 6, 29),
				"오전",
				"좋음",
				"맑음",
				"보통",
				3,
				Map.of("waveHeight", 1.2)
			)));

		String content = service.complete("제주 서핑 추천해줘").content();

		assertThat(content).contains("추천할 만한 장소입니다.", "중문색달해수욕장");
		assertThat(content).contains("파고 1.2m");
		assertThat(content).doesNotContain("waveHeight");
		verify(responsesClient, never()).outputText(any());
	}
}
