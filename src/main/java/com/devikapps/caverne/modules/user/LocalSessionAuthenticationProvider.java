package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class LocalSessionAuthenticationProvider implements AuthenticationProvider {

  private final AuthSessionRepository authSessionRepository;

  @Override
  public String getProviderCode() {
    return AuthProviderCode.LOCAL.name();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean supportsToken(String token) {
    return authSessionRepository.findByToken(token).isPresent();
  }

  @Override
  @Transactional
  public UserAccount authenticate(String token) {
    AuthSession session =
        authSessionRepository
            .findWithUserByToken(token)
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
    authSessionRepository.deleteByToken(token);
  }
}
