package badamoyeo_api.comment.dto;

public record CommentWriterResponse(
	Long userId,
	String nickname,
	String profileImageUrl
) {
}
