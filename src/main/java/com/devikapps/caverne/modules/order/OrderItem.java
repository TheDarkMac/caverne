package com.devikapps.caverne.modules.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  private UUID productId; // Reference to product in catalog module
  private String productLabel;

  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalPrice;
}
