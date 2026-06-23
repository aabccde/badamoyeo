package badamoyeo_api.ingestion.scheduler;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import badamoyeo_api.ai.recommendation.service.AiRecommendationService;
import badamoyeo_api.ingestion.dto.IngestionResult;
import badamoyeo_api.ingestion.service.MarineForecastIngestionService;

@Component
public class MarineForecastIngestionScheduler {
	private static final Logger log = LoggerFactory.getLogger(MarineForecastIngestionScheduler.class);

	private final MarineForecastIngestionService ingestionService;
	private final AiRecommendationService recommendationService;
	private final boolean enabled;
	private final boolean runOnStartup;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public MarineForecastIngestionScheduler(
		MarineForecastIngestionService ingestionService,
		AiRecommendationService recommendationService,
		@Value("${openapi.marine.ingestion.enabled:false}") boolean enabled,
		@Value("${openapi.marine.ingestion.run-on-startup:false}") boolean runOnStartup
	) {
		this.ingestionService = ingestionService;
		this.recommendationService = recommendationService;
		this.enabled = enabled;
		this.runOnStartup = runOnStartup;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void ingestOnStartup() {
		if (!runOnStartup) {
			return;
		}
		ingestLatestForecasts("startup");
	}

	@Scheduled(cron = "${openapi.marine.ingestion.cron:0 10 3,9,15,21 * * *}", zone = "Asia/Seoul")
	public void ingestLatestForecasts() {
		ingestLatestForecasts("schedule");
	}

	private void ingestLatestForecasts(String trigger) {
		if (!enabled) {
			return;
		}
		if (!running.compareAndSet(false, true)) {
			log.info("Skip marine forecast ingestion because another ingestion is already running. trigger={}", trigger);
			return;
		}

		try {
			log.info("Start marine forecast ingestion. trigger={}", trigger);
			LocalDate targetDate = LocalDate.now();
			for (IngestionResult result : ingestionService.ingestAll(targetDate)) {
				refreshRecommendations(result, targetDate, "startup".equals(trigger));
			}
			log.info("Finish marine forecast ingestion. trigger={}", trigger);
		} catch (ResponseStatusException exception) {
			log.warn("Skip marine forecast ingestion. trigger={}, status={}, reason={}",
				trigger,
				exception.getStatusCode(),
				exception.getReason());
		} catch (Exception exception) {
			log.error("Failed marine forecast ingestion. trigger={}", trigger, exception);
		} finally {
			running.set(false);
		}
	}

	private void refreshRecommendations(IngestionResult result, LocalDate targetDate, boolean onlyIfAbsent) {
		if (result.savedCount() <= 0) {
			return;
		}
		try {
			if (onlyIfAbsent) {
				recommendationService.refreshIfAbsent(result.experience(), targetDate);
			} else {
				recommendationService.refresh(result.experience(), targetDate);
			}
			log.info("Refreshed AI spot recommendations for all forecast dates. experience={}, baseDate={}",
				result.experience(), targetDate);
		} catch (Exception exception) {
			log.error("Failed to refresh AI spot recommendations. experience={}, forecastDate={}",
				result.experience(), targetDate, exception);
		}
	}
}
