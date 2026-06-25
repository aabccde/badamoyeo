package badamoyeo_api.upload.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {
	private static final Pattern CATEGORY_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/gif",
		"image/webp"
	);

	private final Path uploadDirectory;
	private final String publicUrlPrefix;
	private final long maxFileSizeBytes;

	public FileStorageService(
		@Value("${app.upload.directory:uploads}") String uploadDirectory,
		@Value("${app.upload.public-url-prefix:/uploads}") String publicUrlPrefix,
		@Value("${app.upload.max-file-size-bytes:5242880}") long maxFileSizeBytes
	) {
		this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
		this.publicUrlPrefix = trimTrailingSlash(publicUrlPrefix);
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<String> storeImages(List<MultipartFile> files, String category) {
		if (files == null || files.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "images are required");
		}
		return files.stream()
			.map(file -> storeImage(file, category))
			.toList();
	}

	public String storeImage(MultipartFile file, String category) {
		validate(file);
		String normalizedCategory = normalizeCategory(category);
		String extension = extension(file.getOriginalFilename());
		String filename = UUID.randomUUID() + "." + extension;
		Path categoryDirectory = uploadDirectory.resolve(normalizedCategory).normalize();
		Path target = categoryDirectory.resolve(filename).normalize();
		if (!target.startsWith(uploadDirectory)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid upload path");
		}

		try {
			Files.createDirectories(categoryDirectory);
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to store image", exception);
		}

		return publicUrlPrefix + "/" + normalizedCategory + "/" + filename;
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image file is required");
		}
		if (file.getSize() > maxFileSizeBytes) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image file is too large");
		}

		String extension = extension(file.getOriginalFilename());
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported image extension");
		}

		String contentType = file.getContentType();
		contentType = contentType == null ? null : contentType.toLowerCase();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported image content type");
		}
	}

	private String normalizeCategory(String category) {
		if (category == null || !CATEGORY_PATTERN.matcher(category).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid upload category");
		}
		return category;
	}

	private String extension(String originalFilename) {
		if (originalFilename == null || !originalFilename.contains(".")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image extension is required");
		}
		String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
		if (extension.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image extension is required");
		}
		return extension;
	}

	private String trimTrailingSlash(String value) {
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}
}
