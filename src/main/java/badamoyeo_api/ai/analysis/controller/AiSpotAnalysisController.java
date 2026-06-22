package badamoyeo_api.ai.analysis.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.service.AiSpotAnalysisService;

@RestController
public class AiSpotAnalysisController {
	private final AiSpotAnalysisService analysisService;

	public AiSpotAnalysisController(AiSpotAnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@GetMapping("/spots/{spotId}/ai-analysis")
	public AiSpotAnalysisResponse analysis(
		@PathVariable Long spotId,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
	) {
		return analysisService.findOrCreate(spotId, targetDate);
	}
}
