package badamoyeo_api.comment.dto;

public class CommentCreateCommand {
	private Long id;
	private Long postId;
	private Long userId;
	private Long parentCommentId;
	private String content;

	public CommentCreateCommand(Long postId, Long userId, Long parentCommentId, String content) {
		this.postId = postId;
		this.userId = userId;
		this.parentCommentId = parentCommentId;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getParentCommentId() {
		return parentCommentId;
	}

	public String getContent() {
		return content;
	}
}
