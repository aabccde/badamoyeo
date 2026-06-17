package badamoyeo_api.post.dto;

public record PostWriterResponse(
	Long userId,
	String nickname,
	String profileImageUrl
) {
}
