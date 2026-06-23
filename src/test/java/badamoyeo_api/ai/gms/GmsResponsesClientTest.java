package badamoyeo_api.ai.gms;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class GmsResponsesClientTest {
	private final GmsResponsesClient client = new GmsResponsesClient(
		new ObjectMapper(),
		"http://localhost/unused",
		"unused"
	);

	@Test
	void reportsIncompleteReasonWhenResponseHasNoOutputText() throws Exception {
		var response = new ObjectMapper().readTree("""
			{
			  "status": "incomplete",
			  "incomplete_details": {"reason": "max_output_tokens"},
			  "output": []
			}
			""");

		assertThatThrownBy(() -> client.outputText(response))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("max_output_tokens");
	}
}
