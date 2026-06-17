package badamoyeo_api.auth.dto;

public record SignupRequest(
	String email,
	String password,
	String nickname
) {
}
