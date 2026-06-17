package badamoyeo_api.user.dto;

public record UserUpdateRequest(
	String nickname,
	String profileImageUrl
) {
}
