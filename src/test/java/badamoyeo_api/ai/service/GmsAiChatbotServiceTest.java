package badamoyeo_api.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.gms.GmsResponsesClient;
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
}
