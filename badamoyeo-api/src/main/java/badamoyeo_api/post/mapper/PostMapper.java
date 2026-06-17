package badamoyeo_api.post.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.post.dto.PostImageRow;
import badamoyeo_api.post.dto.PostInsertCommand;
import badamoyeo_api.post.dto.PostListRow;
import badamoyeo_api.post.dto.PostRow;
import badamoyeo_api.post.dto.PostUpdateRequest;

@Mapper
public interface PostMapper {
	boolean existsActiveSpot(@Param("spotId") Long spotId);

	void insertPost(PostInsertCommand command);

	void updatePost(@Param("postId") Long postId, @Param("request") PostUpdateRequest request);

	void deletePost(@Param("postId") Long postId);

	void incrementSpotPostCount(@Param("spotId") Long spotId);

	void decrementSpotPostCount(@Param("spotId") Long spotId);

	void insertPostImage(@Param("postId") Long postId, @Param("imageUrl") String imageUrl, @Param("sortOrder") int sortOrder);

	void deletePostImages(@Param("postId") Long postId);

	List<PostListRow> findSpotPosts(@Param("spotId") Long spotId, @Param("userId") Long userId,
		@Param("limit") int limit, @Param("offset") int offset);

	long countSpotPosts(@Param("spotId") Long spotId);

	PostRow findPost(@Param("postId") Long postId, @Param("userId") Long userId);

	List<PostImageRow> findPostImages(@Param("postId") Long postId);
}
