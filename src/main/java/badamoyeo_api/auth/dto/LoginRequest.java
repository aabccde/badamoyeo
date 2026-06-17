package badamoyeo_api.auth.dto;

public record LoginRequest(
	String email,
	String password
) {
}
