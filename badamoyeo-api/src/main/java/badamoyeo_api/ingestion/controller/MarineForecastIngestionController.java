package badamoyeo_api.ingestion.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.ingestion.dto.IngestionResult;
import badamoyeo_api.ingestion.service.MarineForecastIngestionService;

@RestController
public class MarineForecastIngestionController {
	private final MarineForecastIngestionService ingestionService;

	public MarineForecastIngestionController(MarineForecastIngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping("/admin/ingest/marine-forecasts")
	public List<IngestionResult> ingest(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
	) {
		return ingestionService.ingestAll(targetDate);
	}
}
