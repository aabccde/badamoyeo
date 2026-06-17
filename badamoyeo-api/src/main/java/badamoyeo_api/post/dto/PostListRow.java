package badamoyeo_api.post.dto;

import java.time.LocalDateTime;

public record PostListRow(
	Long postId,
	String title,
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
