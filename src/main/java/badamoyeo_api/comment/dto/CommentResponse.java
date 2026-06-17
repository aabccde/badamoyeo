package badamoyeo_api.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
	Long commentId,
	Long parentCommentId,
	String content,
	String status,
	CommentWriterResponse writer,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<CommentResponse> children
) {
}
