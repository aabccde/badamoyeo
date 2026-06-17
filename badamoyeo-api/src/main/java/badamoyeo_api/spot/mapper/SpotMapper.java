package badamoyeo_api.spot.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.dashboard.dto.MarkerResponse;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.dto.SpotDetailResponse;
import badamoyeo_api.spot.dto.SpotSearchCondition;

@Mapper
public interface SpotMapper {
	List<MarkerResponse> findMarkers(@Param("experience") String experience, @Param("targetDate") LocalDate targetDate);

	List<SpotCardResponse> findSpotCards(SpotSearchCondition condition);

	long countSpotCards(SpotSearchCondition condition);

	SpotDetailResponse findSpotDetail(@Param("spotId") Long spotId, @Param("targetDate") LocalDate targetDate, @Param("userId") Long userId);

	boolean existsActiveSpot(@Param("spotId") Long spotId);

	void insertFavorite(@Param("spotId") Long spotId, @Param("userId") Long userId);

	void deleteFavorite(@Param("spotId") Long spotId, @Param("userId") Long userId);
}
