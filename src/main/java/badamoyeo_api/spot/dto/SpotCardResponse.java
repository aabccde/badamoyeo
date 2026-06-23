package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SpotCardResponse(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	Integer postCount,
	Boolean favorite,
	String aiReason,
	List<SpotForecastResponse> forecasts
) {
}
