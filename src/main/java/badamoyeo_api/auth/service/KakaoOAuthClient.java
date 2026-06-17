package badamoyeo_api.auth.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.auth.dto.OAuthUserInfo;

@Component
public class KakaoOAuthClient {
	private static final String PROVIDER = "KAKAO";
	private static final String AUTHORIZATION_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";
	private static final String TOKEN_ENDPOINT = "https://kauth.kakao.com/oauth/token";
	private static final String USERINFO_ENDPOINT = "https://kapi.kakao.com/v2/user/me";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public KakaoOAuthClient(
		ObjectMapper objectMapper,
		@Value("${auth.oauth.kakao.client-id:}") String clientId,
		@Value("${auth.oauth.kakao.client-secret:}") String clientSecret,
		@Value("${auth.oauth.kakao.redirect-uri:}") String redirectUri
	) {
		this.restClient = RestClient.create();
		this.objectMapper = objectMapper;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	public URI authorizationUri(String state) {
		validateProperties();
		return UriComponentsBuilder
			.fromUriString(AUTHORIZATION_ENDPOINT)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "profile_nickname")
			.queryParam("state", state)
			.build()
			.encode()
			.toUri();
	}

	public OAuthUserInfo fetchUserInfo(String code) {
		validateProperties();
		if (code == null || code.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
		}

		String accessToken = requestAccessToken(code);
		JsonNode user = requestUserInfo(accessToken);
		String providerId = text(user, "id");
		JsonNode kakaoAccount = user.path("kakao_account");
		JsonNode profile = kakaoAccount.path("profile");
		String email = text(kakaoAccount, "email");
		String nickname = text(profile, "nickname");
		String profileImageUrl = text(profile, "profile_image_url");

		if (providerId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid kakao user info");
		}
		if (email == null) {
			email = "kakao_" + providerId + "@kakao.local";
		}

		return new OAuthUserInfo(PROVIDER, providerId, email, nickname, profileImageUrl);
	}

	private String requestAccessToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);
		if (!clientSecret.isBlank()) {
			form.add("client_secret", clientSecret);
		}

		String response = restClient.post()
			.uri(TOKEN_ENDPOINT)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(String.class);

		JsonNode root = parseJson(response);
		String accessToken = text(root, "access_token");
		if (accessToken == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to get kakao access token");
		}
		return accessToken;
	}

	private JsonNode requestUserInfo(String accessToken) {
		String response = restClient.get()
			.uri(USERINFO_ENDPOINT)
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(String.class);

		return parseJson(response);
	}

	private JsonNode parseJson(String response) {
		if (response == null || response.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "empty kakao oauth response");
		}
		try {
			return objectMapper.readTree(response);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid kakao oauth response", exception);
		}
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text.isBlank() ? null : text;
	}

	private void validateProperties() {
		if (clientId.isBlank() || redirectUri.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kakao oauth properties are required");
		}
	}
}
