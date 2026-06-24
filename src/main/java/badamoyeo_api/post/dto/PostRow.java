package badamoyeo_api.post.dto;

import java.time.LocalDateTime;

public record PostRow(
	Long postId,
	Long spotId,
	String spotName,
	String region,
	Long writerId,
	String writerNickname,
	String writerProfileImageUrl,
	String title,
	String content,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Integer commentCount,
	Integer likeCount,
	Boolean liked
) {
}
