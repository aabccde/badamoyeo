package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SpotDetailResponse(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Boolean favorite,
	Map<String, Object> metrics,
	Map<String, Object> rawData
) {
}
