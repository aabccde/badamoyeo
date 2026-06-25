package badamoyeo_api.ai.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.dto.ChatCompletionResponse;
import badamoyeo_api.ai.dto.AiSpotSearchResult;
import badamoyeo_api.ai.gms.GmsResponsesClient;
import badamoyeo_api.ai.tool.SpotRecommendationTools;

@Service
public class GmsAiChatbotService implements ChatbotService {
	private static final String TOOL_NAME = "searchMarineSpots";
	private static final String OUT_OF_SCOPE_RESPONSE = "바다모여에서는 바다 여행, 해수욕, 갯벌 체험, 스쿠버다이빙, 낚시, 서핑 관련 질문만 답변할 수 있어요.";
	private static final List<String> IN_SCOPE_KEYWORDS = List.of(
		"바다", "해양", "레저", "여행", "해수욕", "해변", "해수욕장", "갯벌", "스쿠버", "다이빙",
		"낚시", "서핑", "물때", "파고", "수온", "풍속", "날씨", "예보", "파도", "조류",
		"제주", "부산", "강원", "강릉", "양양", "속초", "인천", "충남", "태안", "보령",
		"전남", "신안", "무안", "완도", "여수", "통영", "거제", "울릉", "포항", "울진",
		"동해", "서해", "남해"
	);
	private static final List<String> OUT_OF_SCOPE_KEYWORDS = List.of(
		"c언어", "c 언어", "java", "자바", "python", "파이썬", "javascript", "자바스크립트",
		"코딩", "프로그래밍", "알고리즘", "sql", "html", "css", "코드", "컴파일",
		"주식", "코인", "투자", "수학", "영어", "번역", "역사", "요리", "점심", "저녁"
	);
	private static final Map<String, String> METRIC_LABELS = Map.ofEntries(
		Map.entry("airTemperature", "기온"),
		Map.entry("airTemperatureMin", "최저 기온"),
		Map.entry("airTemperatureMax", "최고 기온"),
		Map.entry("waterTemperature", "수온"),
		Map.entry("waterTemperatureMin", "최저 수온"),
		Map.entry("waterTemperatureMax", "최고 수온"),
		Map.entry("waveHeight", "파고"),
		Map.entry("waveHeightMin", "최저 파고"),
		Map.entry("waveHeightMax", "최고 파고"),
		Map.entry("wavePeriod", "파주기"),
		Map.entry("windSpeed", "풍속"),
		Map.entry("windSpeedMin", "최저 풍속"),
		Map.entry("windSpeedMax", "최고 풍속"),
		Map.entry("currentSpeed", "유속"),
		Map.entry("currentSpeedMin", "최저 유속"),
		Map.entry("currentSpeedMax", "최고 유속"),
		Map.entry("openStatus", "개장 상태"),
		Map.entry("targetFish", "대상 어종"),
		Map.entry("availableStartTime", "체험 시작 가능 시간"),
		Map.entry("availableEndTime", "체험 종료 가능 시간"),
		Map.entry("tideStage", "물때"),
		Map.entry("level", "등급")
	);
	private static final String SYSTEM_PROMPT = """
		당신은 대한민국 해양 레저 앱 '바다모여'의 안내 챗봇입니다.
		바다 여행, 해수욕, 갯벌 체험, 스쿠버다이빙, 낚시, 서핑에 관해
		정확하고 다정한 한국어로 답하세요.

		[모바일 답변 원칙]
		- 결론부터 말하고 전체 답변은 500자 이내, 최대 7문장으로 작성하세요.
		- 한 문장은 짧게 쓰고, 인사말·질문 반복·불필요한 서론은 생략하세요.
		- 장소를 나열할 때만 줄바꿈과 '- ' 목록을 사용하고 표와 긴 제목은 쓰지 마세요.
		- 이모지는 사용하지 마세요.
		- 사용자가 요청하지 않은 정보나 후속 제안은 덧붙이지 마세요.
		- 함수 호출 과정, 함수명, 인자, JSON, DB 원문, 영문 필드명은 절대 답변에 노출하지 마세요.
		- '조회하겠습니다' 같은 처리 과정은 말하지 말고 조회가 끝난 최종 답변만 작성하세요.

		[질문 처리]
		1. 일반적인 준비물, 체험 방법, 안전수칙 질문은 알고 있는 범위에서 바로 답하세요.
		2. 답변에 꼭 필요한 지역이나 체험 종류가 없으면 추측하지 말고 한 번에 한 가지만 짧게 물으세요.
		3. 장소 추천·비추천, 특정 지역·장소의 상태나 예보를 묻는 질문은
		   반드시 searchMarineSpots로 바다모여 DB를 먼저 조회하세요.
		4. 좋은 곳과 피할 곳을 함께 요청하면 best와 worst를 각각 조회하세요.
		   추천만 요청하면 best만, 비추천만 요청하면 worst만 조회하세요.
		5. 사용자가 지정한 날짜는 targetDate에 YYYY-MM-DD로 전달하세요.
		   오늘·내일·주말은 입력에 제공된 서버 기준 오늘 날짜로 계산하고,
		   날짜를 지정하지 않은 경우에만 빈 문자열을 전달하세요.

		[DB 답변 규칙]
		- 함수가 반환한 장소명, 날짜, 시간대, 종합지수, 날씨, 물때, 세부 지표만 사용하세요.
		- DB에 없는 장소, 수치, 시설, 교통, 운영시간을 추측하거나 만들지 마세요.
		- 결과가 없으면 '조건에 맞는 예보 데이터를 찾지 못했어요.'라고 말하세요.
		  지역 또는 체험 종류가 빠진 경우에만 필요한 정보 하나를 짧게 물으세요.
		- 기준 날짜는 결론에 한 번만 밝히고, 장소는 중요도순 최대 2곳만 안내하세요.
		- 각 장소는 '장소명(시간대) — 종합지수. 판단 이유' 형식의 한 줄로 쓰세요.
		- 판단 이유에는 해당 체험과 관련 있는 실제 지표를 가능하면 2개 사용하고,
		  수치만 나열하지 말고 그 조건이 체험에 왜 유리하거나 불리한지 친절히 설명하세요.
		- 지표의 의미를 확실히 판단할 수 없으면 과장하지 말고 관찰된 수치만 안내하세요.
		- '매우좋음'과 '좋음'만 추천으로 표현하세요.
		- '보통'은 조건부 방문 가능으로만 표현하세요.
		- '나쁨'과 '매우나쁨'은 추천하지 말고 피해야 할 이유로만 설명하세요.
		- best는 상대 정렬일 뿐이므로 종합지수를 확인한 뒤 추천 여부를 판단하세요.
		- 결과가 모두 '나쁨' 또는 '매우나쁨'이면
		  '해당 날짜에는 추천할 장소가 없습니다.'로 시작하세요.
		- 종합지수가 '나쁨' 또는 '매우나쁨'인 장소에는 '추천', '적합',
		  '도전 가능', '수련 가능' 같은 긍정 표현을 어떤 경우에도 사용하지 마세요.

		[안전]
		- 안전을 보장하거나 실시간 상태를 알고 있는 것처럼 말하지 마세요.
		- 장소·예보 답변의 마지막에는 '방문 전 최신 예보와 현장 통제를 확인하세요.'를 한 번만 쓰세요.
		- 안전 질문에는 구명조끼 착용과 현장 안전수칙 준수를 우선 안내하세요.
		""";

	private final GmsResponsesClient responsesClient;
	private final SpotRecommendationTools spotRecommendationTools;
	private final ObjectMapper objectMapper;
	private final String model;

	public GmsAiChatbotService(
		GmsResponsesClient responsesClient,
		SpotRecommendationTools spotRecommendationTools,
		ObjectMapper objectMapper,
		@Value("${app.ai.chat.model:gpt-5-mini}") String model
	) {
		this.responsesClient = responsesClient;
		this.spotRecommendationTools = spotRecommendationTools;
		this.objectMapper = objectMapper;
		this.model = model;
	}

	@Override
	public ChatCompletionResponse complete(String message) {
		String normalizedMessage = message == null ? "" : message.trim();
		if (isOutOfScope(normalizedMessage)) {
			return new ChatCompletionResponse(OUT_OF_SCOPE_RESPONSE, Instant.now());
		}

		JsonNode firstResponse = responsesClient.execute(initialRequest(normalizedMessage));
		List<ToolSearchResult> toolOutputs = executeToolCalls(firstResponse);
		String content;

		if (toolOutputs.isEmpty()) {
			content = responsesClient.outputText(firstResponse);
		} else {
			content = formatToolAnswer(toolOutputs);
		}

		return new ChatCompletionResponse(normalizeForChatBubble(content), Instant.now());
	}

	private boolean isOutOfScope(String message) {
		if (message.isBlank()) {
			return true;
		}

		String normalized = message.toLowerCase().replaceAll("\\s+", " ");
		boolean inScope = IN_SCOPE_KEYWORDS.stream().anyMatch(normalized::contains);
		boolean explicitlyOutOfScope = OUT_OF_SCOPE_KEYWORDS.stream().anyMatch(normalized::contains);
		if (explicitlyOutOfScope && !inScope) {
			return true;
		}

		return !inScope && normalized.length() > 12;
	}

	private String normalizeForChatBubble(String content) {
		String normalized = content == null ? "" : content.trim();
		if (containsToolInternals(normalized)) {
			return "예보 답변을 정리하는 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.";
		}
		if (normalized.length() <= 500) {
			return normalized;
		}

		int cut = 500;
		for (int index = 499; index >= 380; index--) {
			char character = normalized.charAt(index);
			if (character == '.' || character == '!' || character == '?' || character == '\n') {
				cut = index + 1;
				break;
			}
		}
		return normalized.substring(0, cut).trim();
	}

	private boolean containsToolInternals(String content) {
		return content.contains("\"experience\"")
			|| content.contains("\"targetDate\"")
			|| content.contains("\"spotId\"")
			|| content.contains("\"metrics\"")
			|| content.contains("searchMarineSpots")
			|| content.contains("function_call");
	}

	private Map<String, Object> initialRequest(String message) {
		String datedInput = """
			서버 기준 오늘 날짜: %s
			사용자 질문: %s
			""".formatted(LocalDate.now(), message);
		Map<String, Object> request = responsesClient.baseRequest(model, SYSTEM_PROMPT, datedInput, 700);
		request.put("tools", List.of(searchMarineSpotsTool()));
		return request;
	}

	private List<ToolSearchResult> executeToolCalls(JsonNode response) {
		List<ToolSearchResult> outputs = new ArrayList<>();
		for (JsonNode output : response.path("output")) {
			if (!"function_call".equals(output.path("type").asText())) {
				continue;
			}
			if (!TOOL_NAME.equals(output.path("name").asText())) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GMS requested an unsupported function");
			}
			try {
				JsonNode arguments = objectMapper.readTree(output.path("arguments").asText("{}"));
				List<AiSpotSearchResult> result = spotRecommendationTools.searchMarineSpots(
					arguments.path("experience").asText(""),
					arguments.path("region").asText(""),
					arguments.path("keyword").asText(""),
					arguments.path("targetDate").asText(""),
					arguments.path("sort").asText("best"),
					Math.min(arguments.path("limit").asInt(2), 2)
				);
				outputs.add(new ToolSearchResult(
					arguments.path("sort").asText("best"),
					result
				));
			} catch (ResponseStatusException exception) {
				throw exception;
			} catch (Exception exception) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to execute GMS function call", exception);
			}
		}
		return outputs;
	}

	private String formatToolAnswer(List<ToolSearchResult> toolResults) {
		List<AiSpotSearchResult> items = toolResults.stream()
			.flatMap(result -> result.items().stream())
			.limit(2)
			.toList();
		if (items.isEmpty()) {
			return "조건에 맞는 예보 데이터를 찾지 못했어요.";
		}

		boolean recommendationRequest = toolResults.stream().noneMatch(result -> "worst".equals(result.sort()));
		StringBuilder answer = new StringBuilder();
		if (recommendationRequest && items.stream().noneMatch(this::isRecommended)) {
			answer.append("해당 날짜에는 추천할 장소가 없습니다.");
		} else if (recommendationRequest) {
			answer.append("추천할 만한 장소입니다.");
		} else {
			answer.append("피하는 편이 좋은 장소입니다.");
		}

		for (AiSpotSearchResult item : items) {
			answer.append("\n- ")
				.append(item.spotName())
				.append("(")
				.append(blankToDefault(item.timeSlot(), "예보"))
				.append(") - ")
				.append(blankToDefault(item.totalIndex(), "지수 정보 없음"))
				.append(". ")
				.append(reason(item));
		}
		answer.append("\n방문 전 최신 예보와 현장 통제를 확인하세요.");
		return answer.toString();
	}

	private boolean isRecommended(AiSpotSearchResult item) {
		return "매우좋음".equals(item.totalIndex()) || "좋음".equals(item.totalIndex());
	}

	private String reason(AiSpotSearchResult item) {
		List<String> facts = new ArrayList<>();
		if (item.weather() != null && !item.weather().isBlank()) {
			facts.add("날씨 " + item.weather());
		}
		if (item.metrics() != null) {
			item.metrics().entrySet().stream()
				.filter(entry -> entry.getValue() != null && !isDuplicatedMetric(entry.getKey()))
				.limit(2)
				.map(entry -> metricLabel(entry.getKey()) + " " + metricValue(entry.getKey(), entry.getValue()))
				.forEach(facts::add);
		}
		if (item.tide() != null && !item.tide().isBlank()) {
			facts.add("물때 " + item.tide());
		}
		if (facts.isEmpty()) {
			return "저장된 예보 기준으로 판단했습니다.";
		}
		return String.join(", ", facts.stream().limit(2).toList()) + " 기준으로 판단했습니다.";
	}

	private boolean isDuplicatedMetric(String key) {
		return "weather".equals(key) || "tide".equals(key);
	}

	private String metricLabel(String key) {
		return METRIC_LABELS.getOrDefault(key, key);
	}

	private String metricValue(String key, Object value) {
		String text = String.valueOf(value);
		if (text.isBlank() || text.matches(".*[가-힣a-zA-Z%/].*")) {
			return text;
		}
		if (key.contains("Temperature")) {
			return text + "도";
		}
		if (key.contains("waveHeight")) {
			return text + "m";
		}
		if (key.contains("windSpeed")) {
			return text + "m/s";
		}
		if (key.contains("currentSpeed")) {
			return text + "m/s";
		}
		if ("wavePeriod".equals(key)) {
			return text + "초";
		}
		return text;
	}

	private String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private Map<String, Object> searchMarineSpotsTool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("experience", Map.of(
			"type", "string",
			"description", "체험 종류. seaTravel, swimming, mudflat, scuba, fishing, surfing 중 하나이며 미지정 시 빈 문자열"
		));
		properties.put("region", Map.of(
			"type", "string",
			"description", "지역명. 예: 제주, 부산, 강원. 미지정 시 빈 문자열"
		));
		properties.put("keyword", Map.of(
			"type", "string",
			"description", "장소명 또는 장소명 일부. 미지정 시 빈 문자열"
		));
		properties.put("targetDate", Map.of(
			"type", "string",
			"description", "사용자가 요청한 예보 날짜(YYYY-MM-DD). 날짜를 지정하지 않았으면 빈 문자열"
		));
		properties.put("sort", Map.of(
			"type", "string",
			"enum", List.of("best", "worst", "community"),
			"description", "best 추천순, worst 비추천순, community 게시글 많은 순"
		));
		properties.put("limit", Map.of(
			"type", "integer",
			"minimum", 1,
			"maximum", 2,
			"description", "조회 개수. 최대 2"
		));

		return Map.of(
			"type", "function",
			"name", TOOL_NAME,
			"description", "바다모여 DB에서 지정 날짜 또는 날짜 미지정 시 오늘과 가장 가까운 해양 예보를 검색한다.",
			"strict", true,
			"parameters", Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("experience", "region", "keyword", "targetDate", "sort", "limit"),
				"additionalProperties", false
			)
		);
	}

	private record ToolSearchResult(
		String sort,
		List<AiSpotSearchResult> items
	) {
	}
}
