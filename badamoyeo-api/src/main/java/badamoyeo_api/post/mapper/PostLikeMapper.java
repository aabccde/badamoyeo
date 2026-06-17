package badamoyeo_api.post.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper {
	boolean existsActivePost(@Param("postId") Long postId);

	int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

	int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

	void incrementLikeCount(@Param("postId") Long postId);

	void decrementLikeCount(@Param("postId") Long postId);
}
