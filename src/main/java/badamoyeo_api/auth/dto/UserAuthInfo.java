package badamoyeo_api.auth.dto;

public record UserAuthInfo(
	Long userId,
	String email,
	String password,
	String nickname,
	String profileImageUrl,
	String provider,
	String role,
	String status
) {
}
