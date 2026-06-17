package badamoyeo_api.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.post.service.PostLikeService;

@RestController
public class PostLikeController {
	private final PostLikeService postLikeService;

	public PostLikeController(PostLikeService postLikeService) {
		this.postLikeService = postLikeService;
	}

	@PostMapping("/posts/{postId}/likes")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void like(@PathVariable Long postId, @AuthenticationPrincipal AuthUser authUser) {
		postLikeService.like(postId, authUser.userId());
	}

	@DeleteMapping("/posts/{postId}/likes")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlike(@PathVariable Long postId, @AuthenticationPrincipal AuthUser authUser) {
		postLikeService.unlike(postId, authUser.userId());
	}
}
