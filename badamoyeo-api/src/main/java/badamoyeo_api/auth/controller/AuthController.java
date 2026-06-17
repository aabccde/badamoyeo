package badamoyeo_api.auth.controller;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import badamoyeo_api.auth.dto.AccessTokenResponse;
import badamoyeo_api.auth.dto.AuthResponse;
import badamoyeo_api.auth.dto.LoginRequest;
import badamoyeo_api.auth.dto.LogoutRequest;
import badamoyeo_api.auth.dto.OAuthUserInfo;
import badamoyeo_api.auth.dto.SignupRequest;
import badamoyeo_api.auth.dto.TokenRefreshRequest;
import badamoyeo_api.auth.service.GoogleOAuthClient;
import badamoyeo_api.auth.service.KakaoOAuthClient;
import badamoyeo_api.auth.service.NaverOAuthClient;
import badamoyeo_api.auth.service.AuthService;

@RestController
public class AuthController {
	private final AuthService authService;
	private final GoogleOAuthClient googleOAuthClient;
	private final KakaoOAuthClient kakaoOAuthClient;
	private final NaverOAuthClient naverOAuthClient;
	private final String oauthSuccessRedirectUri;
	private final long refreshTokenDays;
	private final boolean refreshTokenCookieSecure;

	public AuthController(
		AuthService authService,
		GoogleOAuthClient googleOAuthClient,
		KakaoOAuthClient kakaoOAuthClient,
		NaverOAuthClient naverOAuthClient,
		@Value("${auth.oauth.success-redirect-uri:http://localhost:5173/oauth/callback}") String oauthSuccessRedirectUri,
		@Value("${auth.jwt.refresh-token-days}") long refreshTokenDays,
		@Value("${auth.refresh-token-cookie.secure:false}") boolean refreshTokenCookieSecure
	) {
		this.authService = authService;
		this.googleOAuthClient = googleOAuthClient;
		this.kakaoOAuthClient = kakaoOAuthClient;
		this.naverOAuthClient = naverOAuthClient;
		this.oauthSuccessRedirectUri = oauthSuccessRedirectUri;
		this.refreshTokenDays = refreshTokenDays;
		this.refreshTokenCookieSecure = refreshTokenCookieSecure;
	}

	@PostMapping("/auth/signup")
	public ResponseEntity<AccessTokenResponse> signup(@RequestBody SignupRequest request) {
		AuthResponse response = authService.signup(request);
		return tokenResponseWithRefreshCookie(response);
	}

	@PostMapping("/auth/login")
	public ResponseEntity<AccessTokenResponse> login(@RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);
		return tokenResponseWithRefreshCookie(response);
	}

	@PostMapping("/auth/refresh")
	public AccessTokenResponse refresh(
		@RequestBody(required = false) TokenRefreshRequest request,
		@CookieValue(name = "refreshToken", required = false) String refreshTokenCookie
	) {
		AuthResponse response = authService.refresh(resolveRefreshToken(request, refreshTokenCookie));
		return new AccessTokenResponse(response.accessToken(), response.user());
	}

	@PostMapping("/auth/logout")
	public ResponseEntity<Void> logout(
		@RequestBody(required = false) LogoutRequest request,
		@CookieValue(name = "refreshToken", required = false) String refreshTokenCookie
	) {
		authService.logout(resolveRefreshToken(request, refreshTokenCookie));
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie().toString())
			.build();
	}

	@GetMapping("/auth/oauth/{provider}")
	public ResponseEntity<Void> oauthLogin(
		@PathVariable String provider,
		@RequestParam(required = false) String state
	) {
		URI authorizationUri = authorizationUri(provider, state == null ? "" : state);
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(authorizationUri)
			.build();
	}

	@GetMapping("/auth/oauth/{provider}/callback")
	public ResponseEntity<Void> oauthCallback(
		@PathVariable String provider,
		@RequestParam String code,
		@RequestParam(required = false) String state
	) {
		OAuthUserInfo oauthUser = fetchUserInfo(provider, code, state);
		return redirectToFrontend(authService.oauthLogin(oauthUser));
	}

	private URI authorizationUri(String provider, String state) {
		return switch (provider.toLowerCase()) {
			case "google" -> googleOAuthClient.authorizationUri(state);
			case "kakao" -> kakaoOAuthClient.authorizationUri(state);
			case "naver" -> naverOAuthClient.authorizationUri(state);
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported oauth provider");
		};
	}

	private OAuthUserInfo fetchUserInfo(String provider, String code, String state) {
		return switch (provider.toLowerCase()) {
			case "google" -> googleOAuthClient.fetchUserInfo(code);
			case "kakao" -> kakaoOAuthClient.fetchUserInfo(code);
			case "naver" -> naverOAuthClient.fetchUserInfo(code, state);
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported oauth provider");
		};
	}

	private ResponseEntity<Void> redirectToFrontend(AuthResponse response) {
		URI location = UriComponentsBuilder
			.fromUriString(oauthSuccessRedirectUri)
			.build()
			.encode()
			.toUri();
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(location)
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(response.refreshToken()).toString())
			.build();
	}

	private ResponseEntity<AccessTokenResponse> tokenResponseWithRefreshCookie(AuthResponse response) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(response.refreshToken()).toString())
			.body(new AccessTokenResponse(response.accessToken(), response.user()));
	}

	private String resolveRefreshToken(TokenRefreshRequest request, String refreshTokenCookie) {
		if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
			return request.refreshToken();
		}
		return refreshTokenCookie;
	}

	private String resolveRefreshToken(LogoutRequest request, String refreshTokenCookie) {
		if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
			return request.refreshToken();
		}
		return refreshTokenCookie;
	}

	private ResponseCookie refreshTokenCookie(String refreshToken) {
		return ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(refreshTokenCookieSecure)
			.sameSite("Lax")
			.path("/api/auth")
			.maxAge(Duration.ofDays(refreshTokenDays))
			.build();
	}

	private ResponseCookie expiredRefreshTokenCookie() {
		return ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(refreshTokenCookieSecure)
			.sameSite("Lax")
			.path("/api/auth")
			.maxAge(Duration.ZERO)
			.build();
	}
}
