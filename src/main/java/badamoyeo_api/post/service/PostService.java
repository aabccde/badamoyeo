package badamoyeo_api.post.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.common.PageResponse;
import badamoyeo_api.post.dto.PostCreateRequest;
import badamoyeo_api.post.dto.PostCreateResponse;
import badamoyeo_api.post.dto.PostDetailResponse;
import badamoyeo_api.post.dto.PostImageRow;
import badamoyeo_api.post.dto.PostInsertCommand;
import badamoyeo_api.post.dto.PostListRow;
import badamoyeo_api.post.dto.PostListResponse;
import badamoyeo_api.post.dto.PostRow;
import badamoyeo_api.post.dto.PostUpdateRequest;
import badamoyeo_api.post.dto.PostWriterResponse;
import badamoyeo_api.post.mapper.PostMapper;

@Service
public class PostService {
	private final PostMapper postMapper;
	private final int maxImageCount;

	public PostService(
		PostMapper postMapper,
		@Value("${app.post.max-image-count:5}") int maxImageCount
	) {
		this.postMapper = postMapper;
		this.maxImageCount = maxImageCount;
	}

	public PageResponse<PostListResponse> findSpotPosts(Long spotId, int page, int pageSize, Long userId) {
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		List<PostListResponse> items = postMapper.findSpotPosts(spotId, userId, size, (currentPage - 1) * size)
			.stream()
			.map(this::toListResponse)
			.toList();
		long totalCount = postMapper.countSpotPosts(spotId);
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	public PageResponse<PostListResponse> findPosts(String sort, int page, int pageSize, Long userId) {
		String normalizedSort = normalizeSort(sort);
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		List<PostListResponse> items = postMapper.findPosts(normalizedSort, userId, size, (currentPage - 1) * size)
			.stream()
			.map(this::toListResponse)
			.toList();
		long totalCount = postMapper.countPosts();
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	@Transactional
	public PostCreateResponse createPost(Long spotId, Long userId, PostCreateRequest request) {
		requireUser(userId);
		validatePostRequest(request);
		PostCreateRequest normalizedRequest = normalizeCreateRequest(request);
		validateImageUrls(normalizedRequest.imageUrls());
		if (!postMapper.existsActiveSpot(spotId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "spot not found");
		}

		PostInsertCommand command = new PostInsertCommand(spotId, userId, normalizedRequest.title(), normalizedRequest.content());
		postMapper.insertPost(command);
		replaceImages(command.getId(), normalizedRequest.imageUrls());
		postMapper.incrementSpotPostCount(spotId);
		return new PostCreateResponse(command.getId());
	}

	public PostDetailResponse findPostDetail(Long postId, Long userId) {
		PostRow post = findActivePost(postId, userId);
		return toDetail(post);
	}

	@Transactional
	public PostDetailResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
		requireUser(userId);
		validatePostRequest(request);
		PostUpdateRequest normalizedRequest = normalizeUpdateRequest(request);
		validateImageUrls(normalizedRequest.imageUrls());
		PostRow post = findActivePost(postId, userId);
		requireWriter(post, userId);

		postMapper.updatePost(postId, normalizedRequest);
		replaceImages(postId, normalizedRequest.imageUrls());
		return findPostDetail(postId, userId);
	}

	@Transactional
	public void deletePost(Long postId, Long userId) {
		requireUser(userId);
		PostRow post = findActivePost(postId, userId);
		requireWriter(post, userId);

		postMapper.deletePost(postId);
		postMapper.deletePostImages(postId);
		postMapper.decrementSpotPostCount(post.spotId());
	}

	private PostRow findActivePost(Long postId, Long userId) {
		PostRow post = postMapper.findPost(postId, userId);
		if (post == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
		}
		return post;
	}

	private PostDetailResponse toDetail(PostRow post) {
		List<String> imageUrls = postMapper.findPostImages(post.postId()).stream()
			.map(PostImageRow::imageUrl)
			.toList();
		return new PostDetailResponse(
			post.postId(),
			post.spotId(),
			post.spotName(),
			post.region(),
			post.title(),
			post.content(),
			imageUrls,
			new PostWriterResponse(post.writerId(), post.writerNickname(), post.writerProfileImageUrl()),
			post.createdAt(),
			post.updatedAt(),
			post.commentCount(),
			post.likeCount(),
			post.liked()
		);
	}

	private PostListResponse toListResponse(PostListRow post) {
		return new PostListResponse(
			post.postId(),
			post.spotId(),
			post.spotName(),
			post.region(),
			post.title(),
			post.contentPreview(),
			post.thumbnailUrl(),
			new PostWriterResponse(post.writerId(), post.writerNickname(), post.writerProfileImageUrl()),
			post.createdAt(),
			post.commentCount(),
			post.likeCount(),
			post.liked()
		);
	}

	private String normalizeSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return "latest";
		}
		String normalizedSort = sort.trim().toLowerCase();
		if (!List.of("latest", "popular").contains(normalizedSort)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported post sort");
		}
		return normalizedSort;
	}

	private void replaceImages(Long postId, List<String> imageUrls) {
		postMapper.deletePostImages(postId);
		if (imageUrls == null) {
			return;
		}

		for (int i = 0; i < imageUrls.size(); i++) {
			String imageUrl = imageUrls.get(i);
			if (imageUrl != null && !imageUrl.isBlank()) {
				postMapper.insertPostImage(postId, imageUrl.trim(), i);
			}
		}
	}

	private void validateImageUrls(List<String> imageUrls) {
		if (imageUrls == null) {
			return;
		}
		long validImageCount = imageUrls.stream()
			.filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
			.count();
		if (validImageCount > maxImageCount) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "post images must be " + maxImageCount + " or fewer");
		}
	}

	private void validatePostRequest(PostCreateRequest request) {
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
		}
		validatePost(request.title(), request.content());
	}

	private void validatePostRequest(PostUpdateRequest request) {
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
		}
		validatePost(request.title(), request.content());
	}

	private void validatePost(String title, String content) {
		if (title == null || title.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
		}
		if (title.trim().length() > 200) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must be 200 characters or less");
		}
		if (content == null || content.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
		}
	}

	private PostCreateRequest normalizeCreateRequest(PostCreateRequest request) {
		return new PostCreateRequest(request.title().trim(), request.content().trim(), request.imageUrls());
	}

	private PostUpdateRequest normalizeUpdateRequest(PostUpdateRequest request) {
		return new PostUpdateRequest(request.title().trim(), request.content().trim(), request.imageUrls());
	}

	private void requireWriter(PostRow post, Long userId) {
		if (!post.writerId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "writer only");
		}
	}

	private void requireUser(Long userId) {
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization required");
		}
	}
}
