package badamoyeo_api.post.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.post.mapper.PostLikeMapper;

@Service
public class PostLikeService {
	private final PostLikeMapper postLikeMapper;

	public PostLikeService(PostLikeMapper postLikeMapper) {
		this.postLikeMapper = postLikeMapper;
	}

	@Transactional
	public void like(Long postId, Long userId) {
		requirePost(postId);
		int inserted = postLikeMapper.insertLike(postId, userId);
		if (inserted > 0) {
			postLikeMapper.incrementLikeCount(postId);
		}
	}

	@Transactional
	public void unlike(Long postId, Long userId) {
		requirePost(postId);
		int deleted = postLikeMapper.deleteLike(postId, userId);
		if (deleted > 0) {
			postLikeMapper.decrementLikeCount(postId);
		}
	}

	private void requirePost(Long postId) {
		if (!postLikeMapper.existsActivePost(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
		}
	}
}
