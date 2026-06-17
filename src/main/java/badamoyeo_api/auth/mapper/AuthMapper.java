package badamoyeo_api.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import badamoyeo_api.auth.dto.RefreshTokenRecord;
import badamoyeo_api.auth.dto.SignupRequest;
import badamoyeo_api.auth.dto.UserAuthInfo;
import badamoyeo_api.auth.dto.OAuthUserInfo;

@Mapper
public interface AuthMapper {
	boolean existsByEmail(@Param("email") String email);

	boolean existsByNickname(@Param("nickname") String nickname);

	void insertLocalUser(@Param("request") SignupRequest request, @Param("encodedPassword") String encodedPassword);

	void insertOAuthUser(@Param("user") OAuthUserInfo user, @Param("nickname") String nickname);

	UserAuthInfo findByEmail(@Param("email") String email);

	UserAuthInfo findById(@Param("userId") Long userId);

	UserAuthInfo findByProvider(@Param("provider") String provider, @Param("providerId") String providerId);

	void insertRefreshToken(@Param("userId") Long userId, @Param("token") String token, @Param("expiresAt") LocalDateTime expiresAt);

	RefreshTokenRecord findRefreshToken(@Param("token") String token);

	void revokeRefreshToken(@Param("token") String token);
}
