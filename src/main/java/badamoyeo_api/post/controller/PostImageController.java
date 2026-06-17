package badamoyeo_api.post.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.upload.dto.ImagesUploadResponse;
import badamoyeo_api.upload.service.FileStorageService;

@RestController
public class PostImageController {
	private final FileStorageService fileStorageService;
	private final int maxImageCount;

	public PostImageController(
		FileStorageService fileStorageService,
		@Value("${app.post.max-image-count:5}") int maxImageCount
	) {
		this.fileStorageService = fileStorageService;
		this.maxImageCount = maxImageCount;
	}

	@PostMapping("/posts/images")
	public ImagesUploadResponse uploadPostImages(@RequestPart("images") MultipartFile[] images) {
		if (images == null || images.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "images are required");
		}
		if (images.length > maxImageCount) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "post images must be " + maxImageCount + " or fewer");
		}
		return new ImagesUploadResponse(fileStorageService.storeImages(Arrays.asList(images), "posts"));
	}
}
