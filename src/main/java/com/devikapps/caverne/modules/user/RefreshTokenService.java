package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
  private static final int RAW_TOKEN_BYTES = 32;
  private static final SecureRandom RNG = new SecureRandom();

  private final RefreshTokenRepository repository;
  private final RefreshTokenProperties properties;

  public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  public Issued issueForLogin(UserAccount user) {
    UUID familyId = UUID.randomUUID();
    return persist(user, familyId, null);
  }

  public Issued rotate(String rawToken) {
    String hash = hash(rawToken);
    RefreshToken existing =
        repository.findByTokenHash(hash).orElseThrow(() -> unauthorized("Invalid refresh token"));

    if (existing.getRevokedAt() != null) {
      log.warn(
          "Refresh token reuse detected for userId={} familyId={} — revoking family",
          existing.getUser().getId(),
          existing.getFamilyId());
      repository.revokeFamily(existing.getFamilyId());
      throw unauthorized("Refresh token reuse detected");
    }
    if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw unauthorized("Refresh token expired");
    }

    existing.setRevokedAt(LocalDateTime.now());
    return persist(existing.getUser(), existing.getFamilyId(), existing.getId());
  }

  public void revoke(String rawToken) {
    String hash = hash(rawToken);
    repository
        .findByTokenHash(hash)
        .ifPresent(
            token -> {
              if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
              }
            });
  }

  public void revokeAllForUser(UserAccount user) {
    repository.revokeAllForUser(user);
  }

  private Issued persist(UserAccount user, UUID familyId, UUID parentId) {
    String rawToken = randomToken();
    String hash = hash(rawToken);
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(properties.getExpiresInSeconds());

    RefreshToken stored =
        repository.save(
            RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(familyId)
                .parentId(parentId)
                .expiresAt(expiresAt)
                .build());

    return new Issued(user, rawToken, stored.getFamilyId(), expiresAt);
  }

  private String randomToken() {
    byte[] bytes = new byte[RAW_TOKEN_BYTES];
    RNG.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }

  private ResponseStatusException unauthorized(String message) {
    return new ResponseStatusException(UNAUTHORIZED, message);
  }

  public record Issued(UserAccount user, String rawToken, UUID familyId, LocalDateTime expiresAt) {}
}
