package com.devikapps.caverne.modules.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
  List<UserAddress> findAllByUserIdOrderByIdAsc(Long userId);

  Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

  @Modifying
  @Query("update UserAddress address set address.isDefault = false where address.user.id = :userId")
  void clearDefaultForUser(@Param("userId") Long userId);
}
