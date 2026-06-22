package badamoyeo_api.ai.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisRow;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSaveRequest;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;
import badamoyeo_api.ai.analysis.mapper.AiSpotAnalysisMapper;

class AiSpotAnalysisServiceTest {
	private final AiSpotAnalysisMapper mapper = Mockito.mock(AiSpotAnalysisMapper.class);
	private final AiSpotAnalysisGenerator generator = Mockito.mock(AiSpotAnalysisGenerator.class);
	private final AiSpotAnalysisService service = new AiSpotAnalysisService(mapper, generator, new ObjectMapper());

	@Test
	void returnsCachedAnalysisWhenForecastHasNotChanged() {
		LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 23, 9, 10);
		AiSpotAnalysisSource source = source(updatedAt);
		AiSpotAnalysisRow cached = new AiSpotAnalysisRow(
			1L, 10L, "surfing", "중문", LocalDate.of(2026, 6, 23), "오전", "좋음",
			"분석 요약", "[\"파고가 적합합니다.\"]", "[\"현장 통제를 확인해야 합니다.\"]",
			true, "추천할 수 있습니다.", "안전수칙을 확인하세요.", updatedAt,
			LocalDateTime.of(2026, 6, 23, 9, 11)
		);
		when(mapper.findSource(1L, null)).thenReturn(source);
		when(mapper.findAnalysis(10L)).thenReturn(cached);

		AiSpotAnalysisResponse response = service.findOrCreate(1L, null);

		assertThat(response.summary()).isEqualTo("분석 요약");
		assertThat(response.advantages()).containsExactly("파고가 적합합니다.");
		verify(generator, never()).generate(source);
		verify(mapper, never()).upsertAnalysis(Mockito.any());
	}

	@Test
	void regeneratesAnalysisWhenForecastWasUpdated() {
		LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 23, 15, 10);
		AiSpotAnalysisSource source = source(updatedAt);
		AiSpotAnalysisRow stale = new AiSpotAnalysisRow(
			1L, 10L, "surfing", "중문", LocalDate.of(2026, 6, 23), "오전", "보통",
			"이전 분석", "[\"이전 장점\"]", "[\"이전 단점\"]", false, "이전 근거", "이전 안내",
			updatedAt.minusHours(6), updatedAt.minusHours(6)
		);
		AiSpotAnalysisContent generated = new AiSpotAnalysisContent(
			"새 분석", List.of("새 장점"), List.of("새 단점"), true, "새 추천 근거", "새 안전 안내"
		);
		when(mapper.findSource(1L, null)).thenReturn(source);
		when(mapper.findAnalysis(10L)).thenReturn(stale);
		when(generator.generate(source)).thenReturn(generated);

		AiSpotAnalysisResponse response = service.findOrCreate(1L, null);

		assertThat(response.summary()).isEqualTo("새 분석");
		verify(mapper).upsertAnalysis(Mockito.any(AiSpotAnalysisSaveRequest.class));
	}

	private AiSpotAnalysisSource source(LocalDateTime updatedAt) {
		return new AiSpotAnalysisSource(
			1L, 10L, "surfing", "중문", "제주", LocalDate.of(2026, 6, 23),
			"오전", "좋음", "맑음", null, Map.of("waveHeight", 1.2), updatedAt
		);
	}
}
