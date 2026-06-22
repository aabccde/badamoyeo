package badamoyeo_api.ai.analysis.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisRow;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSaveRequest;
import badamoyeo_api.ai.analysis.dto.AiSpotAnalysisSource;

@Mapper
public interface AiSpotAnalysisMapper {
	AiSpotAnalysisSource findSource(
		@Param("spotId") Long spotId,
		@Param("targetDate") LocalDate targetDate
	);

	AiSpotAnalysisRow findAnalysis(@Param("forecastId") Long forecastId);

	void upsertAnalysis(AiSpotAnalysisSaveRequest request);
}
