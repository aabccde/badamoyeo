package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SpotDetailResponse(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	Boolean favorite,
	List<SpotForecastResponse> forecasts
) {
}
