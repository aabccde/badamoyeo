package badamoyeo_api.upload.dto;

import java.util.List;

public record ImagesUploadResponse(
	List<String> imageUrls
) {
}
