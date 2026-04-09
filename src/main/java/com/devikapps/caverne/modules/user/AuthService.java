package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private static final int TOKEN_EXPIRATION_SECONDS = 3600;

  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;
  private final UserApiMapper userApiMapper;
  private final AuthSessionResolver authSessionResolver;
  private final PasswordEncoder passwordEncoder;

  public org.openapitools.client.model.User register(
      org.openapitools.client.model.RegisterRequest input) {
    validateRegistration(input);

    String normalizedEmail = normalizeEmail(input.getEmail());
    String normalizedPhone = normalizePhone(input.getPhone());

    if (normalizedEmail != null
        && userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "email is already registered");
    }
    if (normalizedPhone != null && userRepository.findByPhone(normalizedPhone).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "phone is already registered");
    }

    UserAccount user =
        UserAccount.builder()
            .firstname(input.getFirstname().trim())
            .lastname(input.getLastname().trim())
            .email(normalizedEmail)
            .phone(normalizedPhone)
            .passwordHash(passwordEncoder.encode(input.getPassword()))
            .authProvider(AuthProviderCode.LOCAL)
            .role(UserRole.SIMPLE_USER)
            .status("active")
            .build();

    return userApiMapper.toResponse(userRepository.save(user));
  }

  public org.openapitools.client.model.LoginResponse login(
      org.openapitools.client.model.LoginRequest input) {
    if (input == null
        || isBlank(input.getPassword())
        || (isBlank(input.getEmail()) && isBlank(input.getPhone()))) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "password and either email or phone are required");
    }

    UserAccount user = resolveLoginUser(input);

    if (!passwordEncoder.matches(input.getPassword(), user.getPasswordHash())) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
    }

    String token = UUID.randomUUID() + "." + UUID.randomUUID();
    authSessionRepository.save(
        AuthSession.builder()
            .user(user)
            .token(token)
            .expiresAt(LocalDateTime.now().plusSeconds(TOKEN_EXPIRATION_SECONDS))
            .build());

    return new org.openapitools.client.model.LoginResponse()
        .accessToken(token)
        .tokenType("Bearer")
        .expiresIn(TOKEN_EXPIRATION_SECONDS);
  }

  public void logout(String authorizationHeader) {
    authSessionResolver.logout(authorizationHeader);
  }

  private void validateRegistration(org.openapitools.client.model.RegisterRequest input) {
    if (input == null
        || isBlank(input.getFirstname())
        || isBlank(input.getLastname())
        || isBlank(input.getPassword())
        || (isBlank(input.getEmail()) && isBlank(input.getPhone()))) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY,
          "firstname, lastname, password, and either email or phone are required");
    }
  }

  private UserAccount resolveLoginUser(org.openapitools.client.model.LoginRequest input) {
    String normalizedEmail = normalizeEmail(input.getEmail());
    String normalizedPhone = normalizePhone(input.getPhone());

    UserAccount emailUser =
        normalizedEmail == null
            ? null
            : userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
    UserAccount phoneUser =
        normalizedPhone == null ? null : userRepository.findByPhone(normalizedPhone).orElse(null);

    if (emailUser != null && phoneUser != null && !emailUser.getId().equals(phoneUser.getId())) {
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
    }
    if (emailUser != null) {
      return emailUser;
    }
    if (phoneUser != null) {
      return phoneUser;
    }
    throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
  }

  private String normalizeEmail(String value) {
    if (isBlank(value)) {
      return null;
    }
    return value.trim().toLowerCase();
  }

  private String normalizePhone(String value) {
    if (isBlank(value)) {
      return null;
    }
    return value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
