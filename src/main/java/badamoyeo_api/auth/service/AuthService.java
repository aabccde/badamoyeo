package badamoyeo_api.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import badamoyeo_api.auth.dto.AuthResponse;
import badamoyeo_api.auth.dto.AuthUserResponse;
import badamoyeo_api.auth.dto.LoginRequest;
import badamoyeo_api.auth.dto.LogoutRequest;
import badamoyeo_api.auth.dto.OAuthUserInfo;
import badamoyeo_api.auth.dto.RefreshTokenRecord;
import badamoyeo_api.auth.dto.SignupRequest;
import badamoyeo_api.auth.dto.TokenRefreshRequest;
import badamoyeo_api.auth.dto.UserAuthInfo;
import badamoyeo_api.auth.mapper.AuthMapper;
import badamoyeo_api.auth.security.JwtTokenProvider;

@Service
public class AuthService {
	private static final SecureRandom secureRandom = new SecureRandom();

	private final AuthMapper authMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final long refreshTokenDays;

	public AuthService(
		AuthMapper authMapper,
		PasswordEncoder passwordEncoder,
		JwtTokenProvider jwtTokenProvider,
		@Value("${auth.jwt.refresh-token-days}") long refreshTokenDays
	) {
		this.authMapper = authMapper;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenDays = refreshTokenDays;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		validateSignup(request);
		SignupRequest normalizedRequest = normalizeSignupRequest(request);
		if (authMapper.existsByEmail(normalizedRequest.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
		}
		if (authMapper.existsByNickname(normalizedRequest.nickname())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "nickname already exists");
		}

		authMapper.insertLocalUser(normalizedRequest, passwordEncoder.encode(normalizedRequest.password()));
		UserAuthInfo user = authMapper.findByEmail(normalizedRequest.email());
		return issueTokens(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		if (request == null || isBlank(request.email()) || isBlank(request.password())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password are required");
		}

		LoginRequest normalizedRequest = normalizeLoginRequest(request);
		UserAuthInfo user = authMapper.findByEmail(normalizedRequest.email());
		if (user == null || !"ACTIVE".equals(user.status())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
		}
		if (!"LOCAL".equals(user.provider()) || user.password() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "use oauth login");
		}
		if (!passwordEncoder.matches(normalizedRequest.password(), user.password())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
		}

		return issueTokens(user);
	}

	@Transactional
	public AuthResponse refresh(TokenRefreshRequest request) {
		return refresh(request == null ? null : request.refreshToken());
	}

	@Transactional
	public AuthResponse refresh(String refreshTokenValue) {
		if (isBlank(refreshTokenValue)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
		}

		RefreshTokenRecord refreshToken = authMapper.findRefreshToken(refreshTokenValue);
		if (refreshToken == null || Boolean.TRUE.equals(refreshToken.revoked())
			|| !refreshToken.expiresAt().isAfter(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
		}

		UserAuthInfo user = authMapper.findById(refreshToken.userId());
		if (user == null || !"ACTIVE".equals(user.status())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
		}

		return new AuthResponse(jwtTokenProvider.createAccessToken(user), refreshToken.token(), toUserResponse(user));
	}

	@Transactional
	public void logout(LogoutRequest request) {
		logout(request == null ? null : request.refreshToken());
	}

	@Transactional
	public void logout(String refreshToken) {
		if (isBlank(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
		}
		authMapper.revokeRefreshToken(refreshToken);
	}

	@Transactional
	public AuthResponse oauthLogin(OAuthUserInfo oauthUser) {
		if (oauthUser == null || isBlank(oauthUser.provider()) || isBlank(oauthUser.providerId())
			|| isBlank(oauthUser.email())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth user info is required");
		}

		UserAuthInfo user = authMapper.findByProvider(oauthUser.provider(), oauthUser.providerId());
		if (user != null) {
			if (!"ACTIVE".equals(user.status())) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "inactive user");
			}
			return issueTokens(user);
		}

		UserAuthInfo emailUser = authMapper.findByEmail(oauthUser.email());
		if (emailUser != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered with another login method");
		}

		String nickname = uniqueNickname(oauthUser.nickname(), oauthUser.email());
		authMapper.insertOAuthUser(oauthUser, nickname);
		UserAuthInfo createdUser = authMapper.findByProvider(oauthUser.provider(), oauthUser.providerId());
		return issueTokens(createdUser);
	}

	private AuthResponse issueTokens(UserAuthInfo user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		String refreshToken = createRefreshToken();
		authMapper.insertRefreshToken(user.userId(), refreshToken, LocalDateTime.now().plusDays(refreshTokenDays));
		return new AuthResponse(accessToken, refreshToken, toUserResponse(user));
	}

	private AuthUserResponse toUserResponse(UserAuthInfo user) {
		return new AuthUserResponse(user.userId(), user.email(), user.nickname(), user.profileImageUrl());
	}

	private String createRefreshToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void validateSignup(SignupRequest request) {
		if (request == null || isBlank(request.email()) || isBlank(request.password()) || isBlank(request.nickname())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email, password, nickname are required");
		}
		if (request.password().length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 8 characters");
		}
	}

	private SignupRequest normalizeSignupRequest(SignupRequest request) {
		return new SignupRequest(request.email().trim(), request.password(), request.nickname().trim());
	}

	private LoginRequest normalizeLoginRequest(LoginRequest request) {
		return new LoginRequest(request.email().trim(), request.password());
	}

	private String uniqueNickname(String nickname, String email) {
		String emailName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
		String base = sanitizeNickname(isBlank(nickname) ? emailName : nickname);
		if (base.isBlank()) {
			base = "user";
		}
		if (!authMapper.existsByNickname(base)) {
			return base;
		}
		for (int sequence = 1; sequence <= 1000; sequence++) {
			String candidate = base + sequence;
			if (!authMapper.existsByNickname(candidate)) {
				return candidate;
			}
		}
		throw new ResponseStatusException(HttpStatus.CONFLICT, "failed to create unique nickname");
	}

	private String sanitizeNickname(String value) {
		String sanitized = value == null ? "" : value.replaceAll("[^0-9A-Za-z가-힣_-]", "");
		if (sanitized.length() > 40) {
			return sanitized.substring(0, 40);
		}
		return sanitized;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
