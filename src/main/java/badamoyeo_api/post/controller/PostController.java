package badamoyeo_api.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.common.PageResponse;
import badamoyeo_api.post.dto.PostCreateRequest;
import badamoyeo_api.post.dto.PostCreateResponse;
import badamoyeo_api.post.dto.PostDetailResponse;
import badamoyeo_api.post.dto.PostListResponse;
import badamoyeo_api.post.dto.PostUpdateRequest;
import badamoyeo_api.post.service.PostService;

@RestController
public class PostController {
	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@GetMapping("/posts")
	public PageResponse<PostListResponse> posts(
		@RequestParam(defaultValue = "latest") String sort,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return postService.findPosts(sort, page, pageSize, userId(authUser));
	}

	@GetMapping("/spots/{spotId}/posts")
	public PageResponse<PostListResponse> spotPosts(
		@PathVariable Long spotId,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return postService.findSpotPosts(spotId, page, pageSize, userId(authUser));
	}

	@PostMapping("/spots/{spotId}/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public PostCreateResponse createPost(
		@PathVariable Long spotId,
		@RequestBody PostCreateRequest request,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return postService.createPost(spotId, userId(authUser), request);
	}

	@GetMapping("/posts/{postId}")
	public PostDetailResponse postDetail(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return postService.findPostDetail(postId, userId(authUser));
	}

	@PatchMapping("/posts/{postId}")
	public PostDetailResponse updatePost(
		@PathVariable Long postId,
		@RequestBody PostUpdateRequest request,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return postService.updatePost(postId, userId(authUser), request);
	}

	@DeleteMapping("/posts/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(@PathVariable Long postId, @AuthenticationPrincipal AuthUser authUser) {
		postService.deletePost(postId, userId(authUser));
	}

	private Long userId(AuthUser authUser) {
		return authUser == null ? null : authUser.userId();
	}
}
