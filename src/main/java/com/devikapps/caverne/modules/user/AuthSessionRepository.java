package com.devikapps.caverne.modules.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
  Optional<AuthSession> findByToken(String token);

  @EntityGraph(attributePaths = "user")
  Optional<AuthSession> findWithUserByToken(String token);

  void deleteByToken(String token);

  void deleteByUser(UserAccount user);
}
