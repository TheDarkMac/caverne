package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthSessionResolver {

  private final AuthSessionRepository authSessionRepository;

  @Transactional
  public UserAccount requireUser(String authorizationHeader) {
    String token = extractBearerToken(authorizationHeader);
    AuthSession session =
        authSessionRepository
            .findByToken(token)
            .orElseThrow(
                () -> new ResponseStatusException(UNAUTHORIZED, "Authentication required"));

    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      authSessionRepository.delete(session);
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication token expired");
    }

    return session.getUser();
  }

  @Transactional(readOnly = true)
  public UserAccount requireAdmin(String authorizationHeader) {
    UserAccount user = requireUser(authorizationHeader);
    if (user.getRole() != UserRole.ADMIN) {
      throw new ResponseStatusException(FORBIDDEN, "Admin access required");
    }
    return user;
  }

  @Transactional
  public UserAccount resolveUserOrNull(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return null;
    }
    return requireUser(authorizationHeader);
  }

  public String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
    }
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid authorization header");
    }
    String token = authorizationHeader.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
    }
    return token;
  }
}
