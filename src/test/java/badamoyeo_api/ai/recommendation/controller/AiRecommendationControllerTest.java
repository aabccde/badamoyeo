package badamoyeo_api.ai.recommendation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import badamoyeo_api.ai.recommendation.dto.AiSpotRecommendationResponse;
import badamoyeo_api.ai.recommendation.dto.AiSpotRecommendationsResponse;
import badamoyeo_api.ai.recommendation.service.AiRecommendationService;
import badamoyeo_api.auth.security.JwtAuthenticationFilter;
import badamoyeo_api.common.ApiExceptionHandler;

@WebMvcTest(AiRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AiRecommendationControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiRecommendationService recommendationService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Test
	void returnsLatestRecommendations() throws Exception {
		LocalDate forecastDate = LocalDate.of(2026, 6, 22);
		LocalDateTime generatedAt = LocalDateTime.of(2026, 6, 22, 21, 11);
		AiSpotRecommendationResponse item = new AiSpotRecommendationResponse(
			1L, "surfing", "중문색달해수욕장", "제주", forecastDate, "오전", "좋음",
			10, false, Map.of("waveHeight", 1.2), 1, "파고와 종합지수가 적합합니다.", generatedAt
		);
		when(recommendationService.findRecommendations("surfing", forecastDate, null))
			.thenReturn(new AiSpotRecommendationsResponse("surfing", forecastDate, generatedAt, List.of(item)));

		mockMvc.perform(get("/ai/spot-recommendations")
				.param("experience", "surfing")
				.param("targetDate", "2026-06-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.experience").value("surfing"))
			.andExpect(jsonPath("$.items[0].spotId").value(1))
			.andExpect(jsonPath("$.items[0].rank").value(1))
			.andExpect(jsonPath("$.items[0].reason").value("파고와 종합지수가 적합합니다."));
	}
}
