package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthSessionResolver {

  private final List<AuthenticationProvider> authenticationProviders;

  public UserAccount requireUser(String authorizationHeader) {
    String token = extractBearerToken(authorizationHeader);
    for (AuthenticationProvider provider : authenticationProviders) {
      if (!provider.supportsToken(token)) {
        continue;
      }
      return provider.authenticate(token);
    }
    throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
  }

  public UserAccount requireAdmin(String authorizationHeader) {
    UserAccount user = requireUser(authorizationHeader);
    if (user.getRole() != UserRole.ADMIN) {
      throw new ResponseStatusException(FORBIDDEN, "Admin access required");
    }
    return user;
  }

  public UserAccount resolveUserOrNull(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return null;
    }
    return requireUser(authorizationHeader);
  }

  public void logout(String authorizationHeader) {
    String token = extractBearerToken(authorizationHeader);
    for (AuthenticationProvider provider : authenticationProviders) {
      if (!provider.supportsToken(token)) {
        continue;
      }
      provider.logout(token);
      return;
    }
    throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
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
