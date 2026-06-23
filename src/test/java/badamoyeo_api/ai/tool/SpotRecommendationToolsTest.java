package badamoyeo_api.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import badamoyeo_api.ai.dto.AiSpotSearchCondition;
import badamoyeo_api.ai.mapper.AiSpotMapper;

class SpotRecommendationToolsTest {
	private final AiSpotMapper aiSpotMapper = Mockito.mock(AiSpotMapper.class);
	private final SpotRecommendationTools tools = new SpotRecommendationTools(aiSpotMapper);

	@Test
	void normalizesSearchConditionAndLimitsResultSize() {
		when(aiSpotMapper.searchSpots(any())).thenReturn(List.of());

		tools.searchMarineSpots("surfing", " 제주 ", "", "2026-06-25", "best", 50);

		ArgumentCaptor<AiSpotSearchCondition> captor = ArgumentCaptor.forClass(AiSpotSearchCondition.class);
		verify(aiSpotMapper).searchSpots(captor.capture());
		assertThat(captor.getValue()).isEqualTo(
			new AiSpotSearchCondition("surfing", "제주", null, LocalDate.of(2026, 6, 25), "best", 10)
		);
	}

	@Test
	void ignoresUnsupportedExperienceAndUsesBestSortByDefault() {
		when(aiSpotMapper.searchSpots(any())).thenReturn(List.of());

		tools.searchMarineSpots("unknown", "", "", "", "unknown", 0);

		ArgumentCaptor<AiSpotSearchCondition> captor = ArgumentCaptor.forClass(AiSpotSearchCondition.class);
		verify(aiSpotMapper).searchSpots(captor.capture());
		assertThat(captor.getValue()).isEqualTo(
			new AiSpotSearchCondition(null, null, null, null, "best", 1)
		);
	}
}
