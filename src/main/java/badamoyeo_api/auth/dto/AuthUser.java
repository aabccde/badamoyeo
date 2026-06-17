package badamoyeo_api.auth.dto;

public record AuthUser(
	Long userId,
	String email,
	String nickname,
	String role
) {
}
