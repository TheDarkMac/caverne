package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SupabaseAuthenticationProvider extends AbstractExternalAuthenticationProvider {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final SupabaseAuthProperties properties;
  private final ObjectMapper objectMapper;

  public SupabaseAuthenticationProvider(
      SupabaseAuthProperties properties,
      ObjectMapper objectMapper,
      UserIdentityService userIdentityService) {
    super(userIdentityService);
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getProviderCode() {
    return AuthProviderCode.SUPABASE.name();
  }

  @Override
  public boolean supportsToken(String token) {
    return properties.isEnabled() && token != null && token.chars().filter(ch -> ch == '.').count() == 2;
  }

  @Override
  protected ExternalIdentityProfile verifyIdentity(String token) {
    if (!properties.isEnabled()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase authentication is disabled");
    }
    if (properties.getJwtSecret() == null || properties.getJwtSecret().isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase JWT secret is not configured");
    }

    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid bearer token");
    }

    Map<String, Object> header = decodePart(parts[0]);
    if (!"HS256".equals(header.get("alg"))) {
      throw new ResponseStatusException(
          UNAUTHORIZED, "Unsupported Supabase JWT algorithm");
    }

    verifySignature(parts[0], parts[1], parts[2]);
    Map<String, Object> claims = decodePart(parts[1]);
    validateClaims(claims);

    String subject = stringClaim(claims, "sub");
    if (subject == null || subject.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase token subject is missing");
    }

    Map<String, Object> userMetadata = asMap(claims.get("user_metadata"));
    return new ExternalIdentityProfile(
        AuthProviderCode.SUPABASE,
        subject,
        normalize(stringClaim(claims, "email")),
        normalize(stringClaim(claims, "phone")),
        firstNonBlank(
            stringClaim(userMetadata, "first_name"),
            stringClaim(userMetadata, "given_name"),
            stringClaim(claims, "given_name"),
            stringClaim(claims, "name")),
        firstNonBlank(
            stringClaim(userMetadata, "last_name"),
            stringClaim(userMetadata, "family_name"),
            stringClaim(claims, "family_name")));
  }

  @Override
  public void logout(String token) {
    authenticate(token);
  }

  private void validateClaims(Map<String, Object> claims) {
    long now = Instant.now().getEpochSecond();
    Long expiration = longClaim(claims, "exp");
    if (expiration != null && expiration < now) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase token expired");
    }

    if (properties.getIssuer() != null
        && !properties.getIssuer().isBlank()
        && !properties.getIssuer().equals(stringClaim(claims, "iss"))) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase token issuer is invalid");
    }

    if (properties.getAudience() == null || properties.getAudience().isBlank()) {
      return;
    }

    Object rawAudience = claims.get("aud");
    if (rawAudience instanceof String aud) {
      if (!properties.getAudience().equals(aud)) {
        throw new ResponseStatusException(UNAUTHORIZED, "Supabase token audience is invalid");
      }
      return;
    }
    if (rawAudience instanceof List<?> audiences
        && audiences.stream().map(String::valueOf).noneMatch(properties.getAudience()::equals)) {
      throw new ResponseStatusException(UNAUTHORIZED, "Supabase token audience is invalid");
    }
  }

  private void verifySignature(String encodedHeader, String encodedPayload, String encodedSignature) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] expected = mac.doFinal((encodedHeader + "." + encodedPayload).getBytes(StandardCharsets.UTF_8));
      byte[] actual = Base64.getUrlDecoder().decode(encodedSignature);
      if (!MessageDigest.isEqual(expected, actual)) {
        throw new ResponseStatusException(UNAUTHORIZED, "Supabase token signature is invalid");
      }
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(UNAUTHORIZED, "Unable to verify Supabase token");
    }
  }

  private Map<String, Object> decodePart(String encodedPart) {
    try {
      return objectMapper.readValue(Base64.getUrlDecoder().decode(encodedPart), MAP_TYPE);
    } catch (Exception exception) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid Supabase token");
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
        throw new ResponseStatusException(UNAUTHORIZED, "Invalid Supabase token");
      }
    }
    return null;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
