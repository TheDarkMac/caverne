package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
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
  private static final int CSRF_TOKEN_BYTES = 24;
  private static final SecureRandom RNG = new SecureRandom();

  private final UserRepository userRepository;
  private final UserApiMapper userApiMapper;
  private final PasswordEncoder passwordEncoder;
  private final Optional<SupabaseAdminClient> supabaseAdminClient;
  private final LocalJwtService localJwtService;
  private final LocalJwtProperties localJwtProperties;
  private final RefreshTokenService refreshTokenService;

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

  public AuthIssued login(org.openapitools.client.model.LoginRequest input) {
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

    RefreshTokenService.Issued refresh = refreshTokenService.issueForLogin(user);
    log.info("Login successful userId={} role={}", user.getId(), user.getRole());
    return issueAuthResponse(user, refresh);
  }

  public AuthIssued refresh(String rawRefreshToken) {
    if (isBlank(rawRefreshToken)) {
      throw new ResponseStatusException(UNAUTHORIZED, "Refresh token missing");
    }
    RefreshTokenService.Issued rotated = refreshTokenService.rotate(rawRefreshToken);
    log.info("Refresh rotated userId={} familyId={}", rotated.user().getId(), rotated.familyId());
    return issueAuthResponse(rotated.user(), rotated);
  }

  public void logout(String rawRefreshToken) {
    if (isBlank(rawRefreshToken)) {
      log.info("Logout called without refresh token — nothing to revoke");
      return;
    }
    refreshTokenService.revoke(rawRefreshToken);
    log.info("Logout processed");
  }

  private AuthIssued issueAuthResponse(UserAccount user, RefreshTokenService.Issued refresh) {
    long expiresInSeconds = localJwtProperties.getExpiresInSeconds();
    String jti = UUID.randomUUID().toString();
    String accessToken = localJwtService.issue(user, jti, expiresInSeconds);

    org.openapitools.client.model.LoginResponse body =
        new org.openapitools.client.model.LoginResponse()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn((int) expiresInSeconds);

    return new AuthIssued(
        body, refresh.rawToken(), refresh.expiresAt(), randomCsrfToken());
  }

  private String randomCsrfToken() {
    byte[] bytes = new byte[CSRF_TOKEN_BYTES];
    RNG.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

  public record AuthIssued(
      org.openapitools.client.model.LoginResponse body,
      String rawRefreshToken,
      LocalDateTime refreshExpiresAt,
      String csrfToken) {}
}
