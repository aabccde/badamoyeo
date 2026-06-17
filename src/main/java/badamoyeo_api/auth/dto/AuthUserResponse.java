package badamoyeo_api.auth.dto;

public record AuthUserResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl
) {
}
