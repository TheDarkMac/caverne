package com.devikapps.caverne.modules.catalog;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

  public enum Reason {
    MANUAL_ADJUSTMENT,
    ORDER_PLACED,
    ORDER_CANCELLED,
    RESTOCK
  }

  @Id @UuidGenerator private UUID id;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int delta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private Reason reason;

  @Column(name = "balance_after", nullable = false)
  private int balanceAfter;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(columnDefinition = "TEXT")
  private String note;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
