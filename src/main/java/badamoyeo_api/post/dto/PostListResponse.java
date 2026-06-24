package badamoyeo_api.post.dto;

import java.time.LocalDateTime;

public record PostListResponse(
	Long postId,
	Long spotId,
	String spotName,
	String region,
	String title,
	String contentPreview,
	String thumbnailUrl,
	PostWriterResponse writer,
	LocalDateTime createdAt,
	Integer commentCount,
	Integer likeCount,
	Boolean liked
) {
}
