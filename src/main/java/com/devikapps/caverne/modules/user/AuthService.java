package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final UserApiMapper userApiMapper;
  private final PasswordEncoder passwordEncoder;
  private final Optional<SupabaseAdminClient> supabaseAdminClient;
  private final LocalJwtService localJwtService;
  private final LocalJwtProperties localJwtProperties;

  public org.openapitools.client.model.User register(
      org.openapitools.client.model.RegisterRequest input) {
    validateRegistration(input);

    String normalizedEmail = normalizeEmail(input.getEmail());
    String normalizedPhone = normalizePhone(input.getPhone());

    if (normalizedEmail != null
        && userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "email is already registered");
    }
    if (normalizedPhone != null && userRepository.findByPhone(normalizedPhone).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "phone is already registered");
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

    userRepository.save(user);

    supabaseAdminClient.ifPresent(
        client -> {
          String supabaseId =
              client.createUser(
                  normalizedEmail,
                  normalizedPhone,
                  input.getPassword(),
                  user.getFirstname(),
                  user.getLastname());
          user.setExternalAuthId(supabaseId);
          user.setAuthProvider(AuthProviderCode.SUPABASE);
        });

    log.info(
        "User registered id={} provider={} role={}",
        user.getId(),
        user.getAuthProvider(),
        user.getRole());
    return userApiMapper.toResponse(user);
  }

  public org.openapitools.client.model.LoginResponse login(
      org.openapitools.client.model.LoginRequest input) {
    if (input == null
        || isBlank(input.getPassword())
        || (isBlank(input.getEmail()) && isBlank(input.getPhone()))) {
      throw new ResponseStatusException(
          UNPROCESSABLE_CONTENT, "password and either email or phone are required");
    }

    UserAccount user = resolveLoginUser(input);

    if (!passwordEncoder.matches(input.getPassword(), user.getPasswordHash())) {
      log.warn("Login failed: invalid credentials for userId={}", user.getId());
      throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
    }

    long expiresInSeconds = localJwtProperties.getExpiresInSeconds();
    String jti = UUID.randomUUID().toString();
    String token = localJwtService.issue(user, jti, expiresInSeconds);

    log.info("Login successful userId={} role={} jti={}", user.getId(), user.getRole(), jti);
    return new org.openapitools.client.model.LoginResponse()
        .accessToken(token)
        .tokenType("Bearer")
        .expiresIn((int) expiresInSeconds);
  }

  public void logout(String token) {
    log.info("Logout processed (access JWT is stateless; refresh revocation happens elsewhere)");
  }

  private void validateRegistration(org.openapitools.client.model.RegisterRequest input) {
    if (input == null
        || isBlank(input.getFirstname())
        || isBlank(input.getLastname())
        || isBlank(input.getPassword())
        || (isBlank(input.getEmail()) && isBlank(input.getPhone()))) {
      throw new ResponseStatusException(
          UNPROCESSABLE_CONTENT,
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
