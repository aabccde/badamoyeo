package badamoyeo_api.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.upload.service.FileStorageService;
import badamoyeo_api.user.dto.UserProfileResponse;
import badamoyeo_api.user.service.UserService;

@RestController
public class UserImageController {
	private final FileStorageService fileStorageService;
	private final UserService userService;

	public UserImageController(FileStorageService fileStorageService, UserService userService) {
		this.fileStorageService = fileStorageService;
		this.userService = userService;
	}

	@PostMapping("/users/me/image")
	public UserProfileResponse uploadProfileImage(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestPart("image") MultipartFile image
	) {
		String imageUrl = fileStorageService.storeImage(image, "profiles");
		return userService.updateProfileImage(authUser.userId(), imageUrl);
	}
}
