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
public class NaverOAuthClient {
	private static final String PROVIDER = "NAVER";
	private static final String AUTHORIZATION_ENDPOINT = "https://nid.naver.com/oauth2.0/authorize";
	private static final String TOKEN_ENDPOINT = "https://nid.naver.com/oauth2.0/token";
	private static final String USERINFO_ENDPOINT = "https://openapi.naver.com/v1/nid/me";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public NaverOAuthClient(
		ObjectMapper objectMapper,
		@Value("${auth.oauth.naver.client-id:}") String clientId,
		@Value("${auth.oauth.naver.client-secret:}") String clientSecret,
		@Value("${auth.oauth.naver.redirect-uri:}") String redirectUri
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
			.queryParam("response_type", "code")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("state", state)
			.build()
			.encode()
			.toUri();
	}

	public OAuthUserInfo fetchUserInfo(String code, String state) {
		validateProperties();
		if (code == null || code.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
		}

		String accessToken = requestAccessToken(code, state == null ? "" : state);
		JsonNode root = requestUserInfo(accessToken);
		JsonNode response = root.path("response");
		String providerId = text(response, "id");
		String email = text(response, "email");
		String nickname = text(response, "nickname");
		String profileImageUrl = text(response, "profile_image");

		if (providerId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid naver user info");
		}
		if (email == null) {
			email = "naver_" + providerId + "@naver.local";
		}

		return new OAuthUserInfo(PROVIDER, providerId, email, nickname, profileImageUrl);
	}

	private String requestAccessToken(String code, String state) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("code", code);
		form.add("state", state);

		String response = restClient.post()
			.uri(TOKEN_ENDPOINT)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(String.class);

		JsonNode root = parseJson(response);
		String accessToken = text(root, "access_token");
		if (accessToken == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to get naver access token");
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
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "empty naver oauth response");
		}
		try {
			return objectMapper.readTree(response);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid naver oauth response", exception);
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
		if (clientId.isBlank() || clientSecret.isBlank() || redirectUri.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "naver oauth properties are required");
		}
	}
}
