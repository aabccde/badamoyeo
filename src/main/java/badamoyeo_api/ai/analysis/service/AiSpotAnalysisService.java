package badamoyeo_api.ai.analysis.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisContent;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisResponse;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisRow;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSaveRequest;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;
import badamoyeo_api.ai.analysis.mapper.AiSpotAnalysisMapper;

@Service
public class AiSpotAnalysisService {
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final AiSpotAnalysisMapper analysisMapper;
	private final AiSpotAnalysisGenerator analysisGenerator;
	private final ObjectMapper objectMapper;

	public AiSpotAnalysisService(
		AiSpotAnalysisMapper analysisMapper,
		AiSpotAnalysisGenerator analysisGenerator,
		ObjectMapper objectMapper
	) {
		this.analysisMapper = analysisMapper;
		this.analysisGenerator = analysisGenerator;
		this.objectMapper = objectMapper;
	}

	public synchronized AiSpotAnalysisResponse findOrCreate(Long spotId, LocalDate targetDate) {
		AiSpotAnalysisSource source = analysisMapper.findSource(spotId, targetDate);
		if (source == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot forecast not found");
		}

		AiSpotAnalysisRow cached = analysisMapper.findAnalysis(source.forecastId());
		if (cached != null && source.forecastUpdatedAt().equals(cached.sourceForecastUpdatedAt())) {
			return toResponse(cached);
		}

		AiSpotAnalysisContent generated = validate(analysisGenerator.generate(source));
		LocalDateTime generatedAt = LocalDateTime.now();
		analysisMapper.upsertAnalysis(new AiSpotAnalysisSaveRequest(
			source.spotId(),
			source.forecastId(),
			generated.summary().trim(),
			writeJson(generated.advantages()),
			writeJson(generated.disadvantages()),
			generated.recommended(),
			generated.recommendationReason().trim(),
			generated.safetyNote().trim(),
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
			generated.summary().trim(),
			generated.advantages(),
			generated.disadvantages(),
			generated.recommended(),
			generated.recommendationReason().trim(),
			generated.safetyNote().trim(),
			generatedAt
		);
	}

	private AiSpotAnalysisContent validate(AiSpotAnalysisContent content) {
		if (content == null
			|| isBlank(content.summary())
			|| content.summary().length() > 200
			|| content.advantages() == null || content.advantages().isEmpty() || content.advantages().size() > 2
			|| content.disadvantages() == null || content.disadvantages().isEmpty() || content.disadvantages().size() > 2
			|| isBlank(content.recommendationReason())
			|| content.recommendationReason().length() > 500
			|| isBlank(content.safetyNote())
			|| content.safetyNote().length() > 500
			|| hasInvalidItem(content.advantages())
			|| hasInvalidItem(content.disadvantages())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an invalid spot analysis");
		}
		return content;
	}

	private boolean hasInvalidItem(List<String> values) {
		return values.stream().anyMatch(value -> isBlank(value) || value.length() > 80);
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
			row.summary(),
			readJson(row.advantagesJson()),
			readJson(row.disadvantagesJson()),
			Boolean.TRUE.equals(row.recommended()),
			row.recommendationReason(),
			row.safetyNote(),
			row.generatedAt()
		);
	}

	private String writeJson(List<String> values) {
		try {
			return objectMapper.writeValueAsString(values);
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize spot analysis", exception);
		}
	}

	private List<String> readJson(String value) {
		try {
			return objectMapper.readValue(value, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to read cached spot analysis", exception);
		}
	}
}
