package badamoyeo_api.ai.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisRow;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSaveRequest;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;
import badamoyeo_api.ai.analysis.mapper.AiSpotAnalysisMapper;

class AiSpotAnalysisServiceTest {
	private final AiSpotAnalysisMapper mapper = Mockito.mock(AiSpotAnalysisMapper.class);
	private final AiSpotAnalysisGenerator generator = Mockito.mock(AiSpotAnalysisGenerator.class);
	private final AiSpotAnalysisService service = new AiSpotAnalysisService(mapper, generator);

	@Test
	void returnsCachedAnalysisWhenForecastHasNotChanged() {
		LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 23, 9, 10);
		AiSpotAnalysisSource source = source(updatedAt);
		AiSpotAnalysisRow cached = new AiSpotAnalysisRow(
			1L, 10L, "surfing", "중문", LocalDate.of(2026, 6, 23), "오전", "좋음",
			true, "파고가 안정적이어서 추천할 수 있습니다.", updatedAt,
			LocalDateTime.of(2026, 6, 23, 9, 11)
		);
		when(mapper.findSource(1L, null)).thenReturn(source);
		when(mapper.findAnalysis(10L)).thenReturn(cached);

		AiSpotAnalysisResponse response = service.findOrCreate(1L, null);

		assertThat(response.recommended()).isTrue();
		assertThat(response.recommendationReason()).contains("파고");
		verify(generator, never()).generate(source);
		verify(mapper, never()).upsertAnalysis(Mockito.any());
	}

	@Test
	void regeneratesAnalysisWhenForecastWasUpdated() {
		LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 23, 15, 10);
		AiSpotAnalysisSource source = source(updatedAt);
		AiSpotAnalysisRow stale = new AiSpotAnalysisRow(
			1L, 10L, "surfing", "중문", LocalDate.of(2026, 6, 23), "오전", "보통",
			false, "이전 근거",
			updatedAt.minusHours(6), updatedAt.minusHours(6)
		);
		AiSpotAnalysisContent generated = new AiSpotAnalysisContent(
			true, "파고와 풍속이 안정적이어서 추천합니다."
		);
		when(mapper.findSource(1L, null)).thenReturn(source);
		when(mapper.findAnalysis(10L)).thenReturn(stale);
		when(generator.generate(source)).thenReturn(generated);

		AiSpotAnalysisResponse response = service.findOrCreate(1L, null);

		assertThat(response.recommendationReason()).contains("파고");
		verify(mapper).upsertAnalysis(Mockito.any(AiSpotAnalysisSaveRequest.class));
	}

	private AiSpotAnalysisSource source(LocalDateTime updatedAt) {
		return new AiSpotAnalysisSource(
			1L, 10L, "surfing", "중문", "제주", LocalDate.of(2026, 6, 23),
			"오전", "좋음", "맑음", null, Map.of("waveHeight", 1.2), updatedAt
		);
	}
}
