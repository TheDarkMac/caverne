package com.devikapps.caverne.modules.catalog;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {

  private final StockMovementRepository stockMovementRepository;

  @Transactional
  public StockMovement record(
      UUID productId,
      BigDecimal newBalance,
      BigDecimal delta,
      StockMovement.Reason reason,
      UUID createdBy,
      String note) {
    StockMovement movement =
        StockMovement.builder()
            .productId(productId)
            .delta(delta == null ? 0 : delta.intValue())
            .balanceAfter(newBalance == null ? 0 : newBalance.intValue())
            .reason(reason)
            .createdBy(createdBy)
            .note(note)
            .build();
    return stockMovementRepository.save(movement);
  }

  @Transactional(readOnly = true)
  public Page<StockMovement> listByProduct(UUID productId, Pageable pageable) {
    return stockMovementRepository.findAllByProductIdOrderByCreatedAtDesc(productId, pageable);
  }
}
