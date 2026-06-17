package badamoyeo_api.comment.dto;

public record CommentCreateRequest(
	String content,
	Long parentCommentId
) {
}
