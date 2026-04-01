package com.devikapps.caverne.modules.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  private Long productId; // Reference to product in catalog module
  private String productLabel;

  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalPrice;
}
