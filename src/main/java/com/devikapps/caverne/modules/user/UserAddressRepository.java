package com.devikapps.caverne.modules.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
  List<UserAddress> findAllByUserIdOrderByIdAsc(UUID userId);

  Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

  @Modifying
  @Query("update UserAddress address set address.isDefault = false where address.user.id = :userId")
  void clearDefaultForUser(@Param("userId") UUID userId);
}
