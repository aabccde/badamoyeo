package badamoyeo_api.user.dto;

public record UserSecurityInfo(
	Long userId,
	String password,
	String provider,
	String status
) {
}
