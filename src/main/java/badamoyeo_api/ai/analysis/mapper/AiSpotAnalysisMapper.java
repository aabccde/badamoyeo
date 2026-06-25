package badamoyeo_api.ai.analysis.mapper;

import java.time.LocalDate;
import java.util.List;

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

	AiSpotAnalysisSource findSourceByForecastId(@Param("forecastId") Long forecastId);

	AiSpotAnalysisRow findAnalysis(@Param("forecastId") Long forecastId);

	List<AiSpotAnalysisRow> findFreshAnalyses(@Param("forecastIds") List<Long> forecastIds);

	void upsertAnalysis(AiSpotAnalysisSaveRequest request);
}
