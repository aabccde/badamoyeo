package badamoyeo_api.auth.dto;

import java.time.LocalDateTime;

public record RefreshTokenRecord(
	Long id,
	Long userId,
	String token,
	LocalDateTime expiresAt,
	Boolean revoked
) {
}
