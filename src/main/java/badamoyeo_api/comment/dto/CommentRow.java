package badamoyeo_api.comment.dto;

import java.time.LocalDateTime;

public record CommentRow(
	Long commentId,
	Long postId,
	Long parentCommentId,
	Long writerId,
	String writerNickname,
	String writerProfileImageUrl,
	String content,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
