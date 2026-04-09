package com.devikapps.caverne.modules.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
  Optional<UserAccount> findByAuthProviderAndExternalAuthId(
      AuthProviderCode authProvider, String externalAuthId);

  Optional<UserAccount> findByEmailIgnoreCase(String email);

  Optional<UserAccount> findByPhone(String phone);
}
