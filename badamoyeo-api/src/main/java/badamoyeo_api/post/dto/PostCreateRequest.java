package badamoyeo_api.post.dto;

import java.util.List;

public record PostCreateRequest(
	String title,
	String content,
	List<String> imageUrls
) {
}
