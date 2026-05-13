package com.devikapps.caverne.modules.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Revokes the entire token family in a separate transaction so the revocation is committed even
   * when the caller throws afterwards (reuse-detection path).
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      "update RefreshToken r set r.revokedAt = CURRENT_TIMESTAMP "
          + "where r.familyId = :familyId and r.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId);

  @Modifying
  @Query(
      "update RefreshToken r set r.revokedAt = CURRENT_TIMESTAMP "
          + "where r.user = :user and r.revokedAt is null")
  int revokeAllForUser(@Param("user") UserAccount user);
}
