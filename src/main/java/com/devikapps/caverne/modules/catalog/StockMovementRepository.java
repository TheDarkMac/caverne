package com.devikapps.caverne.modules.catalog;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
  Page<StockMovement> findAllByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}
