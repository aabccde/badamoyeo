package badamoyeo_api.user.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.common.PageResponse;
import badamoyeo_api.post.dto.PostListResponse;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.user.dto.UserDeleteRequest;
import badamoyeo_api.user.dto.UserPasswordChangeRequest;
import badamoyeo_api.user.dto.UserProfileResponse;
import badamoyeo_api.user.dto.UserUpdateRequest;
import badamoyeo_api.user.service.UserService;

@RestController
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users/me")
	public UserProfileResponse me(@AuthenticationPrincipal AuthUser authUser) {
		return userService.findMe(authUser.userId());
	}

	@GetMapping("/users/me/posts")
	public PageResponse<PostListResponse> myPosts(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize
	) {
		return userService.findMyPosts(authUser.userId(), page, pageSize);
	}

	@GetMapping("/users/me/favorite-spots")
	public PageResponse<SpotCardResponse> myFavoriteSpots(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
		@RequestParam(required = false) String timeSlot
	) {
		return userService.findMyFavoriteSpots(authUser.userId(), page, pageSize, targetDate, timeSlot);
	}

	@PatchMapping("/users/me")
	public UserProfileResponse updateMe(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestBody UserUpdateRequest request
	) {
		return userService.updateMe(authUser.userId(), request);
	}

	@PatchMapping("/users/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestBody UserPasswordChangeRequest request
	) {
		userService.changePassword(authUser.userId(), request);
	}

	@DeleteMapping("/users/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMe(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestBody(required = false) UserDeleteRequest request
	) {
		userService.deleteMe(authUser.userId(), request);
	}
}
