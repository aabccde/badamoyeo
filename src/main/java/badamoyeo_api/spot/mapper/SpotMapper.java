package badamoyeo_api.spot.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.spot.dto.SpotCardRow;
import badamoyeo_api.spot.dto.SpotDetailRow;
import badamoyeo_api.spot.dto.SpotForecastRow;
import badamoyeo_api.spot.dto.SpotSearchCondition;

@Mapper
public interface SpotMapper {
	List<MarkerResponse> findMarkers(
		@Param("experience") String experience,
		@Param("targetDate") LocalDate targetDate,
		@Param("timeSlot") String timeSlot
	);

	List<SpotCardRow> findSpotCards(SpotSearchCondition condition);

	long countSpotCards(SpotSearchCondition condition);

	SpotDetailRow findSpotDetail(@Param("spotId") Long spotId, @Param("targetDate") LocalDate targetDate, @Param("userId") Long userId);

	List<SpotForecastRow> findSpotForecasts(
		@Param("spotIds") List<Long> spotIds,
		@Param("targetDate") LocalDate targetDate,
		@Param("timeSlot") String timeSlot
	);

	boolean existsActiveSpot(@Param("spotId") Long spotId);

	void insertFavorite(@Param("spotId") Long spotId, @Param("userId") Long userId);

	void deleteFavorite(@Param("spotId") Long spotId, @Param("userId") Long userId);
}
