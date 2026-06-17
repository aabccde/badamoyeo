package badamoyeo_api.auth.dto;

public record AuthResponse(
	String accessToken,
	String refreshToken,
	AuthUserResponse user
) {
}
