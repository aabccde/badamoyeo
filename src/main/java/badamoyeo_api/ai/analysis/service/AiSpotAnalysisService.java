package badamoyeo_api.ai.analysis.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisRow;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSaveRequest;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;
import badamoyeo_api.ai.analysis.mapper.AiSpotAnalysisMapper;

@Service
public class AiSpotAnalysisService {
	private final AiSpotAnalysisMapper analysisMapper;
	private final AiSpotAnalysisGenerator analysisGenerator;

	public AiSpotAnalysisService(
		AiSpotAnalysisMapper analysisMapper,
		AiSpotAnalysisGenerator analysisGenerator
	) {
		this.analysisMapper = analysisMapper;
		this.analysisGenerator = analysisGenerator;
	}

	public AiSpotAnalysisResponse findOrCreate(Long spotId, LocalDate targetDate) {
		AiSpotAnalysisSource source = analysisMapper.findSource(spotId, targetDate);
		if (source == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot forecast not found");
		}
		return findOrCreate(source);
	}

	public AiSpotAnalysisResponse findOrCreateByForecastId(Long forecastId) {
		AiSpotAnalysisSource source = analysisMapper.findSourceByForecastId(forecastId);
		if (source == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot forecast not found");
		}
		return findOrCreate(source);
	}

	private AiSpotAnalysisResponse findOrCreate(AiSpotAnalysisSource source) {
		AiSpotAnalysisRow cached = analysisMapper.findAnalysis(source.forecastId());
		if (cached != null && source.forecastUpdatedAt().equals(cached.sourceForecastUpdatedAt())) {
			return toResponse(cached);
		}

		AiSpotAnalysisContent generated = validate(analysisGenerator.generate(source));
		LocalDateTime generatedAt = LocalDateTime.now();
		analysisMapper.upsertAnalysis(new AiSpotAnalysisSaveRequest(
			source.spotId(),
			source.forecastId(),
			generated.recommended(),
			generated.recommendationReason().trim(),
			source.forecastUpdatedAt(),
			generatedAt
		));

		return new AiSpotAnalysisResponse(
			source.spotId(),
			source.forecastId(),
			source.experience(),
			source.spotName(),
			source.forecastDate(),
			source.timeSlot(),
			source.totalIndex(),
			generated.recommended(),
			generated.recommendationReason().trim(),
			generatedAt
		);
	}

	private AiSpotAnalysisContent validate(AiSpotAnalysisContent content) {
		if (content == null
			|| isBlank(content.recommendationReason())
			|| content.recommendationReason().length() > 200) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an invalid spot analysis");
		}
		return content;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private AiSpotAnalysisResponse toResponse(AiSpotAnalysisRow row) {
		return new AiSpotAnalysisResponse(
			row.spotId(),
			row.forecastId(),
			row.experience(),
			row.spotName(),
			row.forecastDate(),
			row.timeSlot(),
			row.totalIndex(),
			Boolean.TRUE.equals(row.recommended()),
			row.recommendationReason(),
			row.generatedAt()
		);
	}
}
