package badamoyeo_api.ingestion.dto;

import java.math.BigDecimal;

public record SpotUpsertRequest(
	String experience,
	String name,
	BigDecimal lat,
	BigDecimal lng,
	String externalPlaceCode,
	String externalPlaceName,
	String region
) {
}
