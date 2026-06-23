package badamoyeo_api.user.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.post.dto.PostListRow;
import badamoyeo_api.spot.dto.SpotCardRow;
import badamoyeo_api.user.dto.UserSecurityInfo;
import badamoyeo_api.user.dto.UserProfileResponse;
import badamoyeo_api.user.dto.UserUpdateRequest;

@Mapper
public interface UserMapper {
	UserProfileResponse findProfile(@Param("userId") Long userId);

	boolean existsNicknameOwnedByOtherUser(@Param("userId") Long userId, @Param("nickname") String nickname);

	void updateProfile(@Param("userId") Long userId, @Param("request") UserUpdateRequest request);

	void updateProfileImage(@Param("userId") Long userId, @Param("profileImageUrl") String profileImageUrl);

	void deleteUser(@Param("userId") Long userId);

	UserSecurityInfo findSecurityInfo(@Param("userId") Long userId);

	void updatePassword(@Param("userId") Long userId, @Param("encodedPassword") String encodedPassword);

	List<PostListRow> findMyPosts(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

	long countMyPosts(@Param("userId") Long userId);

	List<SpotCardRow> findMyFavoriteSpots(@Param("userId") Long userId, @Param("targetDate") LocalDate targetDate,
		@Param("timeSlot") String timeSlot, @Param("limit") int limit, @Param("offset") int offset);

	long countMyFavoriteSpots(
		@Param("userId") Long userId,
		@Param("targetDate") LocalDate targetDate,
		@Param("timeSlot") String timeSlot
	);
}
