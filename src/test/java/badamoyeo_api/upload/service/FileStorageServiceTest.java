package badamoyeo_api.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class FileStorageServiceTest {
	@TempDir
	Path uploadDirectory;

	@Test
	void storesImageUnderCategoryDirectory() {
		FileStorageService service = new FileStorageService(uploadDirectory.toString(), "/uploads", 1024);
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"photo.PNG",
			"IMAGE/PNG",
			new byte[] {1, 2, 3}
		);

		String imageUrl = service.storeImage(file, "profiles");

		assertThat(imageUrl).startsWith("/uploads/profiles/");
		assertThat(Files.exists(uploadDirectory.resolve("profiles"))).isTrue();
	}

	@Test
	void rejectsPathTraversalCategory() {
		FileStorageService service = new FileStorageService(uploadDirectory.toString(), "/uploads", 1024);
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"photo.png",
			"image/png",
			new byte[] {1, 2, 3}
		);

		assertThatThrownBy(() -> service.storeImage(file, "../outside"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("invalid upload category");
	}

	@Test
	void rejectsBlankExtension() {
		FileStorageService service = new FileStorageService(uploadDirectory.toString(), "/uploads", 1024);
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"photo.",
			"image/png",
			new byte[] {1, 2, 3}
		);

		assertThatThrownBy(() -> service.storeImage(file, "profiles"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("image extension is required");
	}
}
