package badamoyeo_api.comment.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.comment.dto.CommentCreateCommand;
import badamoyeo_api.comment.dto.CommentCreateRequest;
import badamoyeo_api.comment.dto.CommentCreateResponse;
import badamoyeo_api.comment.dto.CommentResponse;
import badamoyeo_api.comment.dto.CommentRow;
import badamoyeo_api.comment.dto.CommentUpdateRequest;
import badamoyeo_api.comment.dto.CommentWriterResponse;
import badamoyeo_api.comment.mapper.CommentMapper;

@Service
public class CommentService {
	private static final String DELETED_CONTENT = "삭제된 댓글입니다.";

	private final CommentMapper commentMapper;

	public CommentService(CommentMapper commentMapper) {
		this.commentMapper = commentMapper;
	}

	public List<CommentResponse> findComments(Long postId) {
		if (!commentMapper.existsActivePost(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
		}

		List<CommentRow> rows = commentMapper.findCommentsByPost(postId);
		Map<Long, MutableComment> comments = new LinkedHashMap<>();
		for (CommentRow row : rows) {
			comments.put(row.commentId(), new MutableComment(row));
		}

		List<MutableComment> roots = new ArrayList<>();
		for (MutableComment comment : comments.values()) {
			Long parentCommentId = comment.row.parentCommentId();
			if (parentCommentId == null || !comments.containsKey(parentCommentId)) {
				roots.add(comment);
				continue;
			}
			comments.get(parentCommentId).children.add(comment);
		}

		return roots.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public CommentCreateResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
		validateContent(request == null ? null : request.content());
		if (!commentMapper.existsActivePost(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
		}

		Long parentCommentId = request.parentCommentId();
		if (parentCommentId != null) {
			CommentRow parent = commentMapper.findComment(parentCommentId);
			if (parent == null || !"ACTIVE".equals(parent.status()) || !parent.postId().equals(postId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid parent comment");
			}
			if (parent.parentCommentId() != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nested replies are not supported");
			}
		}

		CommentCreateCommand command = new CommentCreateCommand(postId, userId, parentCommentId, request.content().trim());
		commentMapper.insertComment(command);
		commentMapper.incrementPostCommentCount(postId);
		return new CommentCreateResponse(command.getId());
	}

	@Transactional
	public CommentResponse updateComment(Long commentId, Long userId, CommentUpdateRequest request) {
		validateContent(request == null ? null : request.content());
		CommentRow comment = requireActiveComment(commentId);
		requireWriter(comment, userId);

		commentMapper.updateComment(commentId, request.content().trim());
		CommentRow updated = requireComment(commentId);
		return toResponse(new MutableComment(updated));
	}

	@Transactional
	public void deleteComment(Long commentId, Long userId) {
		CommentRow comment = requireActiveComment(commentId);
		requireWriter(comment, userId);

		commentMapper.deleteComment(commentId);
		commentMapper.decrementPostCommentCount(comment.postId());
	}

	private CommentRow requireComment(Long commentId) {
		CommentRow comment = commentMapper.findComment(commentId);
		if (comment == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found");
		}
		return comment;
	}

	private CommentRow requireActiveComment(Long commentId) {
		CommentRow comment = requireComment(commentId);
		if (!"ACTIVE".equals(comment.status())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found");
		}
		return comment;
	}

	private void requireWriter(CommentRow comment, Long userId) {
		if (!comment.writerId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "writer only");
		}
	}

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
		}
	}

	private CommentResponse toResponse(MutableComment comment) {
		CommentRow row = comment.row;
		boolean deleted = "DELETED".equals(row.status());
		return new CommentResponse(
			row.commentId(),
			row.parentCommentId(),
			deleted ? DELETED_CONTENT : row.content(),
			row.status(),
			deleted ? null : new CommentWriterResponse(row.writerId(), row.writerNickname(), row.writerProfileImageUrl()),
			row.createdAt(),
			row.updatedAt(),
			comment.children.stream().map(this::toResponse).toList()
		);
	}

	private static class MutableComment {
		private final CommentRow row;
		private final List<MutableComment> children = new ArrayList<>();

		private MutableComment(CommentRow row) {
			this.row = row;
		}
	}
}
