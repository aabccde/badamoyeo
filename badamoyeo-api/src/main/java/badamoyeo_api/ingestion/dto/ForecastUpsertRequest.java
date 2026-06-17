package badamoyeo_api.ingestion.dto;

import java.time.LocalDate;

public record ForecastUpsertRequest(
	Long spotId,
	String experience,
	LocalDate forecastDate,
	String timeSlot,
	String totalIndex,
	String weather,
	String tide,
	String variantKey,
	String metricsJson,
	String rawDataJson
) {
}
