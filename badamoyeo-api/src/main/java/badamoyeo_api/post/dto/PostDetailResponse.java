package badamoyeo_api.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
	Long postId,
	Long spotId,
	String title,
	String content,
	List<String> imageUrls,
	PostWriterResponse writer,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Integer commentCount,
	Integer likeCount,
	Boolean liked
) {
}
