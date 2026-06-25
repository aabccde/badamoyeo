package badamoyeo_api.ai.recommendation.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.ai.recommendation.dto.AiRecommendationCandidate;
import badamoyeo_api.ai.recommendation.dto.AiRecommendationSaveRequest;

@Mapper
public interface AiRecommendationMapper {
	List<LocalDate> findRecommendationForecastDates(
		@Param("experience") String experience,
		@Param("baseDate") LocalDate baseDate
	);

	boolean existsRecommendations(
		@Param("experience") String experience,
		@Param("forecastDate") LocalDate forecastDate
	);

	List<AiRecommendationCandidate> findCandidates(
		@Param("experience") String experience,
		@Param("forecastDate") LocalDate forecastDate,
		@Param("limit") int limit
	);

	void deleteRecommendations(
		@Param("experience") String experience,
		@Param("forecastDate") LocalDate forecastDate
	);

	void insertRecommendations(@Param("recommendations") List<AiRecommendationSaveRequest> recommendations);
}
