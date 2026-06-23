package badamoyeo_api.spot.dto;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ForecastTimeSlot {
	private static final Set<String> SUPPORTED_VALUES = Set.of("오전", "오후", "일");

	private ForecastTimeSlot() {
	}

	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (!SUPPORTED_VALUES.contains(normalized)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported timeSlot: " + value);
		}
		return normalized;
	}
}
