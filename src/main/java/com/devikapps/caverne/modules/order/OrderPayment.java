package com.devikapps.caverne.modules.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderPayment {

  private UUID paymentId;

  private String methodCode;
  private String currencyCode;
  private BigDecimal amount;
  private LocalDateTime date;
  private String status;

  @Column(name = "internal_reference")
  private String internalReference;

  @Column(columnDefinition = "TEXT")
  private String providerResponse;
}
