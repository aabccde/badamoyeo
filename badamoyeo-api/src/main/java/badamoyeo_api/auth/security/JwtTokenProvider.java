package badamoyeo_api.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import badamoyeo_api.auth.dto.AuthUser;
import badamoyeo_api.auth.dto.UserAuthInfo;

@Component
public class JwtTokenProvider {
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;
	private final byte[] secret;
	private final long accessTokenSeconds;

	public JwtTokenProvider(
		ObjectMapper objectMapper,
		@Value("${auth.jwt.secret}") String secret,
		@Value("${auth.jwt.access-token-minutes}") long accessTokenMinutes
	) {
		this.objectMapper = objectMapper;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.accessTokenSeconds = accessTokenMinutes * 60;
	}

	public String createAccessToken(UserAuthInfo user) {
		Instant now = Instant.now();
		return createToken(Map.of(
			"sub", user.userId().toString(),
			"email", user.email(),
			"nickname", user.nickname(),
			"role", user.role(),
			"iat", now.getEpochSecond(),
			"exp", now.plusSeconds(accessTokenSeconds).getEpochSecond()
		));
	}

	public AuthUser parseAccessToken(String token) {
		Map<String, Object> payload = parsePayload(token);
		long expiresAt = number(payload.get("exp"));
		if (Instant.now().getEpochSecond() >= expiresAt) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "access token expired");
		}

		return new AuthUser(
			Long.valueOf(payload.get("sub").toString()),
			payload.get("email").toString(),
			payload.get("nickname").toString(),
			payload.get("role").toString()
		);
	}

	public List<SimpleGrantedAuthority> authorities(AuthUser authUser) {
		return List.of(new SimpleGrantedAuthority("ROLE_" + authUser.role()));
	}

	private String createToken(Map<String, Object> payload) {
		try {
			String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
			String body = encodeJson(payload);
			String unsignedToken = header + "." + body;
			return unsignedToken + "." + sign(unsignedToken);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to create access token", exception);
		}
	}

	private Map<String, Object> parsePayload(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access token");
			}

			String unsignedToken = parts[0] + "." + parts[1];
			String expectedSignature = sign(unsignedToken);
			if (!constantTimeEquals(expectedSignature, parts[2])) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access token");
			}

			String json = new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
			return objectMapper.readValue(json, MAP_TYPE);
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access token", exception);
		}
	}

	private String encodeJson(Map<String, Object> value) throws Exception {
		return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
	}

	private String sign(String value) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "HmacSHA256"));
		return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
	}

	private boolean constantTimeEquals(String left, String right) {
		return MessageDigestSupport.constantTimeEquals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
	}

	private long number(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(value.toString());
	}

	private static class MessageDigestSupport {
		static boolean constantTimeEquals(byte[] left, byte[] right) {
			if (left.length != right.length) {
				return false;
			}
			int result = 0;
			for (int i = 0; i < left.length; i++) {
				result |= left[i] ^ right[i];
			}
			return result == 0;
		}
	}
}
