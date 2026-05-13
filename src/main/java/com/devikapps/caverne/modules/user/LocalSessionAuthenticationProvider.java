package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Order(0)
@RequiredArgsConstructor
public class LocalSessionAuthenticationProvider implements AuthenticationProvider {

  private final UserRepository userRepository;
  private final LocalJwtService localJwtService;

  @Override
  public String getProviderCode() {
    return AuthProviderCode.LOCAL.name();
  }

  @Override
  public boolean supportsToken(String token) {
    if (token == null || token.chars().filter(ch -> ch == '.').count() != 2) {
      return false;
    }
    Map<String, Object> claims = localJwtService.peekClaims(token);
    return Objects.equals(localJwtService.getIssuer(), claims.get("iss"));
  }

  @Override
  @Transactional(readOnly = true)
  public UserAccount authenticate(String token) {
    Map<String, Object> claims = localJwtService.verify(token);

    Object sub = claims.get("sub");
    if (!(sub instanceof String subValue) || subValue.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Token is missing sub");
    }

    UUID userId;
    try {
      userId = UUID.fromString(subValue);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid token subject");
    }

    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authentication required"));
  }

  @Override
  public void logout(String token) {
    // Access JWT is stateless — revocation happens on the refresh token (see RefreshTokenService).
  }
}
