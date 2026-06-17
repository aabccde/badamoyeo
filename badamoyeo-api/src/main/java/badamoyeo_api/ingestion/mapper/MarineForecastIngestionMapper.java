package badamoyeo_api.ingestion.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import badamoyeo_api.ingestion.dto.ForecastUpsertRequest;
import badamoyeo_api.ingestion.dto.SpotIdLookup;
import badamoyeo_api.ingestion.dto.SpotUpsertRequest;

@Mapper
public interface MarineForecastIngestionMapper {
	Long findSpotId(SpotUpsertRequest request);

	void insertSpot(SpotUpsertRequest request);

	void updateSpot(SpotUpsertRequest request);

	void upsertForecast(ForecastUpsertRequest request);

	void upsertSpots(@Param("spots") List<SpotUpsertRequest> spots);

	List<SpotIdLookup> findSpotIds(@Param("spots") List<SpotUpsertRequest> spots);

	void upsertForecasts(@Param("forecasts") List<ForecastUpsertRequest> forecasts);
}
