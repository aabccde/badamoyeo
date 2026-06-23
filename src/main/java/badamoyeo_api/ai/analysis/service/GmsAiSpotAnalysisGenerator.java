package badamoyeo_api.ai.analysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;
import badamoyeo_api.ai.gms.GmsResponsesClient;

@Service
public class GmsAiSpotAnalysisGenerator implements AiSpotAnalysisGenerator {
	private static final String SYSTEM_PROMPT = """
		당신은 바다모여의 해양 레저 장소 분석가입니다.
		제공된 DB 데이터만 사용하고 시설, 교통, 운영시간 또는 수치를 추측하지 마세요.
		종합지수와 체험별 지표를 함께 분석하되 안전을 보장한다고 표현하지 마세요.
		recommended는 종합지수가 '매우좋음' 또는 '좋음'일 때만 true로 판단하세요.
		'보통'은 조건부 가능, '나쁨'과 '매우나쁨'은 false로 판단하세요.
		recommendationReason은 실제 지표를 1~2개 사용해 한국어 1문장, 120자 이내로 작성하세요.
		반드시 {"recommended":true,"recommendationReason":"이유"} 형태의 JSON만 반환하세요.
		""";

	private final GmsResponsesClient responsesClient;
	private final ObjectMapper objectMapper;
	private final String model;

	public GmsAiSpotAnalysisGenerator(
		GmsResponsesClient responsesClient,
		ObjectMapper objectMapper,
		@Value("${app.ai.analysis.model:gpt-5-mini}") String model
	) {
		this.responsesClient = responsesClient;
		this.objectMapper = objectMapper;
		this.model = model;
	}

	@Override
	public AiSpotAnalysisContent generate(AiSpotAnalysisSource source) {
		try {
			return responsesClient.completeJson(
				model,
				SYSTEM_PROMPT,
				"""
					다음 장소의 해당 예보를 분석하세요.
					recommended는 이 체험을 위해 방문을 추천할 수 있는 데이터인지 판단한 값입니다.

					%s
					""".formatted(objectMapper.writeValueAsString(source)),
				800,
				AiSpotAnalysisContent.class
			);
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize analysis source", exception);
		}
	}
}
