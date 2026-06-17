package badamoyeo_api.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl,
	String provider,
	LocalDateTime createdAt
) {
}
