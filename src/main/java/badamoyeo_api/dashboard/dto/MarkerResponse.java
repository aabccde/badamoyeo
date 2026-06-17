package badamoyeo_api.dashboard.dto;

import java.math.BigDecimal;

public record MarkerResponse(
	Long spotId,
	String spotName,
	BigDecimal lat,
	BigDecimal lng,
	String totalIndex,
	String experience
) {
}
