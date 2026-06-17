package badamoyeo_api.auth.dto;

public record OAuthUserInfo(
	String provider,
	String providerId,
	String email,
	String nickname,
	String profileImageUrl
) {
}
