package badamoyeo_api.post.dto;

import java.util.List;

public record PostUpdateRequest(
	String title,
	String content,
	List<String> imageUrls
) {
}
