package com.devikapps.caverne.modules.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Price {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(name = "currency_code", length = 3)
  private String currencyCode;

  @Column(precision = 19, scale = 4)
  private BigDecimal value;

  private LocalDate validFrom;

  private String unit;
}
