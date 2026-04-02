package com.devikapps.caverne.modules.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
  Optional<AuthSession> findByToken(String token);

  void deleteByToken(String token);

  void deleteByUser(UserAccount user);
}
