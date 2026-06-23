package badamoyeo_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JacksonConfigTest {
	@Test
	void serializesJavaTimeValuesAsIsoStrings() throws Exception {
		String json = new JacksonConfig().objectMapper()
			.writeValueAsString(Map.of("forecastDate", LocalDate.of(2026, 6, 23)));

		assertThat(json).isEqualTo("{\"forecastDate\":\"2026-06-23\"}");
	}
}
