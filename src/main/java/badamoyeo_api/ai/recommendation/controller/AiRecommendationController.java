package badamoyeo_api.ai.recommendation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.ai.recommendation.dto.AiSpotRecommendationsResponse;
import badamoyeo_api.ai.recommendation.service.AiRecommendationService;
import badamoyeo_api.auth.dto.AuthUser;

@RestController
@RequestMapping("/ai/spot-recommendations")
public class AiRecommendationController {
	private final AiRecommendationService recommendationService;

	public AiRecommendationController(AiRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping
	public AiSpotRecommendationsResponse recommendations(
		@RequestParam String experience,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return recommendationService.findRecommendations(
			experience,
			targetDate,
			authUser == null ? null : authUser.userId()
		);
	}
}
