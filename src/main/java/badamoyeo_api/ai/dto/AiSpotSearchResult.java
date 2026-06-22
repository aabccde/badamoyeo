package badamoyeo_api.ai.dto;

import java.time.LocalDate;
import java.util.Map;

public record AiSpotSearchResult(
	Long spotId,
	String spotName,
	String experience,
	String region,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	Integer postCount,
	Map<String, Object> metrics
) {
}
