package badamoyeo_api.spot.dto;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Experience {
	SEA_TRAVEL("seaTravel"),
	SWIMMING("swimming"),
	MUDFLAT("mudflat"),
	SCUBA("scuba"),
	FISHING("fishing"),
	SURFING("surfing");

	private final String apiValue;

	Experience(String apiValue) {
		this.apiValue = apiValue;
	}

	public String apiValue() {
		return apiValue;
	}

	public static Experience from(String value) {
		return Arrays.stream(values())
			.filter(experience -> experience.apiValue.equals(value))
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported experience: " + value));
	}
}
