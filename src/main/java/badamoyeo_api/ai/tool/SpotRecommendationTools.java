package badamoyeo_api.ai.tool;

import java.util.List;
import java.util.Set;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import badamoyeo_api.ai.dto.AiSpotSearchCondition;
import badamoyeo_api.ai.dto.AiSpotSearchResult;
import badamoyeo_api.ai.mapper.AiSpotMapper;

@Component
public class SpotRecommendationTools {
	private static final Set<String> EXPERIENCES = Set.of(
		"seaTravel", "swimming", "mudflat", "scuba", "fishing", "surfing"
	);
	private static final Set<String> SORTS = Set.of("best", "worst", "community");

	private final AiSpotMapper aiSpotMapper;

	public SpotRecommendationTools(AiSpotMapper aiSpotMapper) {
		this.aiSpotMapper = aiSpotMapper;
	}

	@Tool(description = """
		바다모여 데이터베이스에서 실제 장소와 가장 가까운 날짜의 해양 예보를 검색한다.
		장소 추천, 비추천, 어디가 좋은지 또는 나쁜지, 특정 지역·장소의 상태를 묻는 질문에는
		반드시 이 도구를 먼저 호출해야 한다. 반환된 장소와 수치만 답변 근거로 사용한다.
		""")
	public List<AiSpotSearchResult> searchMarineSpots(
		@ToolParam(description = "체험 종류. seaTravel, swimming, mudflat, scuba, fishing, surfing 중 하나. 특정하지 않으면 빈 문자열")
		String experience,
		@ToolParam(description = "지역명. 예: 제주, 부산, 강원. 특정하지 않으면 빈 문자열")
		String region,
		@ToolParam(description = "정확하거나 일부인 장소명. 특정하지 않으면 빈 문자열")
		String keyword,
		@ToolParam(description = "best는 추천순, worst는 비추천순, community는 게시글 많은 순")
		String sort,
		@ToolParam(description = "조회 개수. 1부터 10까지")
		int limit
	) {
		return aiSpotMapper.searchSpots(new AiSpotSearchCondition(
			normalizeExperience(experience),
			normalize(region),
			normalize(keyword),
			SORTS.contains(sort) ? sort : "best",
			Math.min(Math.max(limit, 1), 10)
		));
	}

	private String normalizeExperience(String experience) {
		String normalized = normalize(experience);
		return EXPERIENCES.contains(normalized) ? normalized : null;
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
