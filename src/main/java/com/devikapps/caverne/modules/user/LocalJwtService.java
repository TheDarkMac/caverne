package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalJwtService {

  private static final Logger log = LoggerFactory.getLogger(LocalJwtService.class);
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final LocalJwtProperties properties;
  private final ObjectMapper objectMapper;

  public LocalJwtService(LocalJwtProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  void warnIfDevSecret() {
    String secret = properties.getSecret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "auth.local.jwt.secret must be configured (AUTH_LOCAL_JWT_SECRET)");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      log.warn(
          "auth.local.jwt.secret is shorter than 32 bytes — HS256 requires at least 256 bits for"
              + " production use");
    }
  }

  public String issue(UserAccount user, String jti, long expiresInSeconds) {
    long now = Instant.now().getEpochSecond();
    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("iss", properties.getIssuer());
    claims.put("sub", user.getId().toString());
    claims.put("jti", jti);
    claims.put("iat", now);
    claims.put("exp", now + expiresInSeconds);
    claims.put("role", user.getRole().name());
    if (user.getEmail() != null) {
      claims.put("email", user.getEmail());
    }
    if (user.getPhone() != null) {
      claims.put("phone", user.getPhone());
    }

    String encodedHeader = encode(header);
    String encodedPayload = encode(claims);
    String signature = sign(encodedHeader + "." + encodedPayload);
    return encodedHeader + "." + encodedPayload + "." + signature;
  }

  /** Decodes the JWT payload without verifying the signature. Use only for cheap pre-checks. */
  public Map<String, Object> peekClaims(String token) {
    if (token == null) {
      return Map.of();
    }
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(URL_DECODER.decode(parts[1]), MAP_TYPE);
    } catch (Exception exception) {
      return Map.of();
    }
  }

  public String getIssuer() {
    return properties.getIssuer();
  }

  /** Verifies signature, algorithm, issuer and expiration; returns claims on success. */
  public Map<String, Object> verify(String token) {
    String[] parts = token == null ? new String[0] : token.split("\\.");
    if (parts.length != 3) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid bearer token");
    }

    Map<String, Object> header = decodePart(parts[0]);
    if (!"HS256".equals(header.get("alg"))) {
      throw new ResponseStatusException(UNAUTHORIZED, "Unsupported JWT algorithm");
    }

    verifySignature(parts[0], parts[1], parts[2]);
    Map<String, Object> claims = decodePart(parts[1]);

    if (!properties.getIssuer().equals(stringClaim(claims, "iss"))) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid token issuer");
    }
    Long expiration = longClaim(claims, "exp");
    if (expiration == null || expiration < Instant.now().getEpochSecond()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Token expired");
    }
    return claims;
  }

  private void verifySignature(String header, String payload, String signature) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] expected = mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8));
      byte[] actual = URL_DECODER.decode(signature);
      if (!MessageDigest.isEqual(expected, actual)) {
        throw new ResponseStatusException(UNAUTHORIZED, "Invalid token signature");
      }
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(UNAUTHORIZED, "Unable to verify token");
    }
  }

  private String sign(String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign JWT", exception);
    }
  }

  private String encode(Object value) {
    try {
      return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to encode JWT segment", exception);
    }
  }

  private Map<String, Object> decodePart(String encoded) {
    try {
      return objectMapper.readValue(URL_DECODER.decode(encoded), MAP_TYPE);
    } catch (Exception exception) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid bearer token");
    }
  }

  private String stringClaim(Map<String, Object> claims, String name) {
    Object value = claims.get(name);
    return value == null ? null : String.valueOf(value);
  }

  private Long longClaim(Map<String, Object> claims, String name) {
    Object value = claims.get(name);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException exception) {
        throw new ResponseStatusException(UNAUTHORIZED, "Invalid token claim");
      }
    }
    return null;
  }
}
