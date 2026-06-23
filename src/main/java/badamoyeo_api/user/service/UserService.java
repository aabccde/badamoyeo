package badamoyeo_api.user.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.common.PageResponse;
import badamoyeo_api.post.dto.PostListResponse;
import badamoyeo_api.post.dto.PostListRow;
import badamoyeo_api.post.dto.PostWriterResponse;
import badamoyeo_api.spot.dto.SpotCardResponse;
import badamoyeo_api.spot.dto.SpotCardRow;
import badamoyeo_api.spot.dto.ForecastTimeSlot;
import badamoyeo_api.spot.service.SpotService;
import badamoyeo_api.user.dto.UserDeleteRequest;
import badamoyeo_api.user.dto.UserPasswordChangeRequest;
import badamoyeo_api.user.dto.UserProfileResponse;
import badamoyeo_api.user.dto.UserSecurityInfo;
import badamoyeo_api.user.dto.UserUpdateRequest;
import badamoyeo_api.user.mapper.UserMapper;

@Service
public class UserService {
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final SpotService spotService;

	public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, SpotService spotService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.spotService = spotService;
	}

	public UserProfileResponse findMe(Long userId) {
		requireUser(userId);
		UserProfileResponse profile = userMapper.findProfile(userId);
		if (profile == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
		}
		return profile;
	}

	public PageResponse<PostListResponse> findMyPosts(Long userId, int page, int pageSize) {
		requireUser(userId);
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		List<PostListResponse> items = userMapper.findMyPosts(userId, size, (currentPage - 1) * size)
			.stream()
			.map(this::toPostListResponse)
			.toList();
		long totalCount = userMapper.countMyPosts(userId);
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	public PageResponse<SpotCardResponse> findMyFavoriteSpots(
		Long userId,
		int page,
		int pageSize,
		LocalDate targetDate,
		String timeSlot
	) {
		requireUser(userId);
		int currentPage = Math.max(page, 1);
		int size = Math.min(Math.max(pageSize, 1), 100);
		LocalDate date = effectiveTargetDate(targetDate);
		String normalizedTimeSlot = ForecastTimeSlot.normalize(timeSlot);
		List<SpotCardRow> rows = userMapper.findMyFavoriteSpots(
			userId,
			date,
			normalizedTimeSlot,
			size,
			(currentPage - 1) * size
		);
		List<SpotCardResponse> items = spotService.attachForecasts(rows, date, normalizedTimeSlot);
		long totalCount = userMapper.countMyFavoriteSpots(userId, date, normalizedTimeSlot);
		return PageResponse.of(items, currentPage, size, totalCount);
	}

	@Transactional
	public UserProfileResponse updateMe(Long userId, UserUpdateRequest request) {
		requireUser(userId);
		validateNicknameUpdate(userId, request);
		UserUpdateRequest normalizedRequest = normalizeProfileUpdate(request);
		if (!hasProfileUpdate(normalizedRequest)) {
			return findMe(userId);
		}
		userMapper.updateProfile(userId, normalizedRequest);
		return findMe(userId);
	}

	@Transactional
	public UserProfileResponse updateProfileImage(Long userId, String profileImageUrl) {
		requireUser(userId);
		if (isBlank(profileImageUrl)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profileImageUrl is required");
		}
		userMapper.updateProfileImage(userId, profileImageUrl);
		return findMe(userId);
	}

	@Transactional
	public void changePassword(Long userId, UserPasswordChangeRequest request) {
		requireUser(userId);
		UserSecurityInfo user = requireSecurityInfo(userId);
		if (!"LOCAL".equals(user.provider())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth user cannot change password");
		}
		if (request == null || isBlank(request.newPassword()) || isBlank(request.newPasswordConfirm())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newPassword and newPasswordConfirm are required");
		}
		if (!request.newPassword().equals(request.newPasswordConfirm())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password confirmation does not match");
		}
		if (request.newPassword().length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 8 characters");
		}
		userMapper.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
	}

	@Transactional
	public void deleteMe(Long userId, UserDeleteRequest request) {
		requireUser(userId);
		UserSecurityInfo user = requireSecurityInfo(userId);
		if ("LOCAL".equals(user.provider())) {
			if (request == null || isBlank(request.password())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
			}
			if (user.password() == null || !passwordEncoder.matches(request.password(), user.password())) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid password");
			}
		}
		userMapper.deleteUser(userId);
	}

	private void requireUser(Long userId) {
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization required");
		}
	}

	private UserSecurityInfo requireSecurityInfo(Long userId) {
		UserSecurityInfo user = userMapper.findSecurityInfo(userId);
		if (user == null || !"ACTIVE".equals(user.status())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
		}
		return user;
	}

	private boolean hasProfileUpdate(UserUpdateRequest request) {
		return request != null
			&& ((request.nickname() != null && !request.nickname().isBlank())
				|| request.profileImageUrl() != null);
	}

	private void validateNicknameUpdate(Long userId, UserUpdateRequest request) {
		if (request == null || request.nickname() == null) {
			return;
		}
		if (request.nickname().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname must not be blank");
		}
		String nickname = request.nickname().trim();
		if (userMapper.existsNicknameOwnedByOtherUser(userId, nickname)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "nickname already exists");
		}
	}

	private UserUpdateRequest normalizeProfileUpdate(UserUpdateRequest request) {
		if (request == null) {
			return null;
		}
		String nickname = request.nickname() == null ? null : request.nickname().trim();
		return new UserUpdateRequest(nickname, request.profileImageUrl());
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private LocalDate effectiveTargetDate(LocalDate targetDate) {
		return targetDate == null ? LocalDate.now() : targetDate;
	}

	private PostListResponse toPostListResponse(PostListRow post) {
		return new PostListResponse(
			post.postId(),
			post.title(),
			post.thumbnailUrl(),
			new PostWriterResponse(post.writerId(), post.writerNickname(), post.writerProfileImageUrl()),
			post.createdAt(),
			post.commentCount(),
			post.likeCount(),
			post.liked()
		);
	}
}
