package badamoyeo_api.comment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.comment.dto.CommentCreateCommand;
import badamoyeo_api.comment.dto.CommentRow;

@Mapper
public interface CommentMapper {
	boolean existsActivePost(@Param("postId") Long postId);

	void insertComment(CommentCreateCommand command);

	CommentRow findComment(@Param("commentId") Long commentId);

	List<CommentRow> findCommentsByPost(@Param("postId") Long postId);

	void updateComment(@Param("commentId") Long commentId, @Param("content") String content);

	void deleteComment(@Param("commentId") Long commentId);

	void incrementPostCommentCount(@Param("postId") Long postId);

	void decrementPostCommentCount(@Param("postId") Long postId);
}
