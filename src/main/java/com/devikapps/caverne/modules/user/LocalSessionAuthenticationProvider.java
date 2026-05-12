package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Order(0)
@RequiredArgsConstructor
public class LocalSessionAuthenticationProvider implements AuthenticationProvider {

  private final AuthSessionRepository authSessionRepository;
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
  @Transactional
  public UserAccount authenticate(String token) {
    Map<String, Object> claims = localJwtService.verify(token);

    Object jti = claims.get("jti");
    if (!(jti instanceof String jtiValue) || jtiValue.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Token is missing jti");
    }

    AuthSession session =
        authSessionRepository
            .findWithUserByToken(jtiValue)
            .orElseThrow(
                () -> new ResponseStatusException(UNAUTHORIZED, "Authentication required"));

    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      authSessionRepository.delete(session);
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication token expired");
    }

    UserAccount user = session.getUser();
    user.getRole();
    return user;
  }

  @Override
  @Transactional
  public void logout(String token) {
    Map<String, Object> claims = localJwtService.peekClaims(token);
    Object jti = claims.get("jti");
    if (jti instanceof String jtiValue && !jtiValue.isBlank()) {
      authSessionRepository.deleteByToken(jtiValue);
    }
  }
}
