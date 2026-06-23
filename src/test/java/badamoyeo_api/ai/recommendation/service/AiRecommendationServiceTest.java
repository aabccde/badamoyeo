package badamoyeo_api.ai.recommendation.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationItem;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationSaveRequest;
import badamoyeo_api.ai.recommendation.mapper.AiRecommendationMapper;

class AiRecommendationServiceTest {
	private final AiRecommendationMapper mapper = Mockito.mock(AiRecommendationMapper.class);
	private final AiRecommendationGenerator generator = Mockito.mock(AiRecommendationGenerator.class);
	private final TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);
	private final AiRecommendationService service =
		new AiRecommendationService(mapper, generator, transactionTemplate);

	@Test
	void replacesRecommendationsOnlyAfterValidAiResponse() {
		LocalDate date = LocalDate.of(2026, 6, 22);
		List<AiRecommendationCandidate> candidates = candidates();
		List<AiRecommendationItem> generated = IntStream.rangeClosed(1, 6)
			.mapToObj(rank -> new AiRecommendationItem((long) rank, rank, "추천 이유 " + rank))
			.toList();
		when(mapper.findRecommendationForecastDates("surfing", date)).thenReturn(List.of(date));
		when(mapper.findCandidates("surfing", date, 20)).thenReturn(candidates);
		when(generator.generate("surfing", candidates)).thenReturn(generated);
		Mockito.doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Consumer<TransactionStatus> callback = invocation.getArgument(0);
			callback.accept(Mockito.mock(TransactionStatus.class));
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());

		service.refresh("surfing", date);

		verify(mapper).deleteRecommendations("surfing", date);
		ArgumentCaptor<List<AiRecommendationSaveRequest>> captor = ArgumentCaptor.forClass(List.class);
		verify(mapper).insertRecommendations(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue()).hasSize(6);
	}

	@Test
	void keepsExistingRecommendationsWhenAiReturnsUnknownSpot() {
		LocalDate date = LocalDate.of(2026, 6, 22);
		List<AiRecommendationCandidate> candidates = candidates();
		List<AiRecommendationItem> generated = List.of(
			new AiRecommendationItem(999L, 1, "잘못된 장소"),
			new AiRecommendationItem(2L, 2, "이유"),
			new AiRecommendationItem(3L, 3, "이유"),
			new AiRecommendationItem(4L, 4, "이유"),
			new AiRecommendationItem(5L, 5, "이유"),
			new AiRecommendationItem(6L, 6, "이유")
		);
		when(mapper.findRecommendationForecastDates("surfing", date)).thenReturn(List.of(date));
		when(mapper.findCandidates("surfing", date, 20)).thenReturn(candidates);
		when(generator.generate("surfing", candidates)).thenReturn(generated);

		assertThatThrownBy(() -> service.refresh("surfing", date))
			.isInstanceOf(ResponseStatusException.class);

		verify(mapper, never()).deleteRecommendations(any(), any());
		verify(mapper, never()).insertRecommendations(any());
	}

	@Test
	void skipsAiGenerationOnStartupWhenRecommendationsAlreadyExist() {
		LocalDate date = LocalDate.of(2026, 6, 22);
		when(mapper.findRecommendationForecastDates("surfing", date)).thenReturn(List.of(date));
		when(mapper.existsRecommendations("surfing", date)).thenReturn(true);

		service.refreshIfAbsent("surfing", date);

		verify(mapper, never()).findCandidates(any(), any(), eq(20));
		verify(generator, never()).generate(any(), any());
	}

	@Test
	void generatesRecommendationsForEveryForecastDate() {
		LocalDate baseDate = LocalDate.of(2026, 6, 23);
		LocalDate nextDate = baseDate.plusDays(1);
		List<AiRecommendationCandidate> firstCandidates = candidatesFor(baseDate);
		List<AiRecommendationCandidate> secondCandidates = candidatesFor(nextDate);
		List<AiRecommendationItem> generated = IntStream.rangeClosed(1, 6)
			.mapToObj(rank -> new AiRecommendationItem((long) rank, rank, "추천 이유 " + rank))
			.toList();
		when(mapper.findRecommendationForecastDates("surfing", baseDate))
			.thenReturn(List.of(baseDate, nextDate));
		when(mapper.findCandidates("surfing", baseDate, 20)).thenReturn(firstCandidates);
		when(mapper.findCandidates("surfing", nextDate, 20)).thenReturn(secondCandidates);
		when(generator.generate("surfing", firstCandidates)).thenReturn(generated);
		when(generator.generate("surfing", secondCandidates)).thenReturn(generated);
		Mockito.doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Consumer<TransactionStatus> callback = invocation.getArgument(0);
			callback.accept(Mockito.mock(TransactionStatus.class));
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());

		service.refresh("surfing", baseDate);

		verify(generator).generate("surfing", firstCandidates);
		verify(generator).generate("surfing", secondCandidates);
		verify(mapper).deleteRecommendations("surfing", baseDate);
		verify(mapper).deleteRecommendations("surfing", nextDate);
	}

	private List<AiRecommendationCandidate> candidates() {
		return candidatesFor(LocalDate.of(2026, 6, 22));
	}

	private List<AiRecommendationCandidate> candidatesFor(LocalDate forecastDate) {
		return IntStream.rangeClosed(1, 6)
			.mapToObj(id -> new AiRecommendationCandidate(
				(long) id,
				"장소 " + id,
				"제주",
				forecastDate,
				"오전",
				"좋음",
				"맑음",
				null,
				id,
				Map.of("waveHeight", 0.8)
			))
			.toList();
	}
}
