package badamoyeo_api.spot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SpotCardResponse(
	Long spotId,
	String experience,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	Integer postCount,
	Boolean favorite,
	Map<String, Object> metrics
) {
}
