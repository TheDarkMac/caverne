package com.devikapps.caverne.modules.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, UUID> {
  Optional<OrderPayment> findByInternalReference(String internalReference);

  Optional<OrderPayment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
