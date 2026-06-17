package badamoyeo_api.user.dto;

public record UserPasswordChangeRequest(
	String newPassword,
	String newPasswordConfirm
) {
}
