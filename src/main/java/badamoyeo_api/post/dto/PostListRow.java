package badamoyeo_api.post.dto;

import java.time.LocalDateTime;

public record PostListRow(
	Long postId,
	Long spotId,
	String spotName,
	String region,
	String title,
	String contentPreview,
	String thumbnailUrl,
	Long writerId,
	String writerNickname,
	String writerProfileImageUrl,
	LocalDateTime createdAt,
	Integer commentCount,
	Integer likeCount,
	Boolean liked
) {
}
