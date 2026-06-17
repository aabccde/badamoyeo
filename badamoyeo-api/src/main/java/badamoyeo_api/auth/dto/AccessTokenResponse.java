package badamoyeo_api.auth.dto;

public record AccessTokenResponse(
	String accessToken,
	AuthUserResponse user
) {
}
