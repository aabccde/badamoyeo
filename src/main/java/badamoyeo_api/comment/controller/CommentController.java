package badamoyeo_api.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.comment.dto.CommentCreateRequest;
import badamoyeo_api.comment.dto.CommentCreateResponse;
import badamoyeo_api.comment.dto.CommentResponse;
import badamoyeo_api.comment.dto.CommentUpdateRequest;
import badamoyeo_api.comment.service.CommentService;

@RestController
public class CommentController {
	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping("/posts/{postId}/comments")
	public List<CommentResponse> comments(@PathVariable Long postId) {
		return commentService.findComments(postId);
	}

	@PostMapping("/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentCreateResponse createComment(
		@PathVariable Long postId,
		@RequestBody CommentCreateRequest request,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return commentService.createComment(postId, authUser.userId(), request);
	}

	@PatchMapping("/comments/{commentId}")
	public CommentResponse updateComment(
		@PathVariable Long commentId,
		@RequestBody CommentUpdateRequest request,
		@AuthenticationPrincipal AuthUser authUser
	) {
		return commentService.updateComment(commentId, authUser.userId(), request);
	}

	@DeleteMapping("/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal AuthUser authUser) {
		commentService.deleteComment(commentId, authUser.userId());
	}
}
