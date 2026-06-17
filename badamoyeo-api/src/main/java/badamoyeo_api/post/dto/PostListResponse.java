package badamoyeo_api.post.dto;

import java.time.LocalDateTime;

public record PostListResponse(
	Long postId,
	String title,
	String thumbnailUrl,
	PostWriterResponse writer,
	LocalDateTime createdAt,
	Integer commentCount,
	Integer likeCount,
	Boolean liked
) {
}
