package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public org.openapitools.client.model.User register(
      org.openapitools.client.model.RegisterRequest input) {
    validateRegistration(input);

    if (userRepository.findByEmailIgnoreCase(input.getEmail()).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "email is already registered");
    }

    UserAccount user =
        UserAccount.builder()
            .firstname(input.getFirstname().trim())
            .lastname(input.getLastname().trim())
            .email(input.getEmail().trim().toLowerCase())
            .phone(input.getPhone())
            .passwordHash(passwordEncoder.encode(input.getPassword()))
            .role(UserRole.SIMPLE_USER)
            .status("active")
            .build();

    return userApiMapper.toResponse(userRepository.save(user));
  }

  public org.openapitools.client.model.LoginResponse login(
      org.openapitools.client.model.LoginRequest input) {
    if (input == null || isBlank(input.getEmail()) || isBlank(input.getPassword())) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "email and password are required");
    }

    UserAccount user =
        userRepository
            .findByEmailIgnoreCase(input.getEmail().trim())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

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
    String token = authSessionResolver.extractBearerToken(authorizationHeader);
    if (authSessionRepository.findByToken(token).isEmpty()) {
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
    }
    authSessionRepository.deleteByToken(token);
  }

  private void validateRegistration(org.openapitools.client.model.RegisterRequest input) {
    if (input == null
        || isBlank(input.getFirstname())
        || isBlank(input.getLastname())
        || isBlank(input.getEmail())
        || isBlank(input.getPassword())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "firstname, lastname, email, and password are required");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
