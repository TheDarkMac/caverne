package com.devikapps.caverne.modules.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
    extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
  Optional<Order> findByIdAndUserId(UUID id, UUID userId);

  @Query(
      value =
          """
          select o.*
          from orders o
          join order_payments op on op.order_id = o.id
          where lower(op.internal_reference) = lower(:internalReference)
          limit 1
          """,
      nativeQuery = true)
  Optional<Order> findByPaymentInternalReference(
      @Param("internalReference") String internalReference);
}
