package com.devikapps.caverne.modules.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "order_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderPayment {

  @Id private UUID paymentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  @ToString.Exclude
  private Order order;

  private String methodCode;
  private String currencyCode;
  private BigDecimal amount;
  private LocalDateTime date;
  private String status;

  @Column(name = "internal_reference")
  private String internalReference;

  @Column(columnDefinition = "TEXT")
  private String providerResponse;

  @Column(name = "refund_id")
  private String refundId;

  @Column(name = "refunded_amount")
  private BigDecimal refundedAmount;

  @Column(name = "refunded_at")
  private OffsetDateTime refundedAt;

  @Column(name = "refund_reason", columnDefinition = "TEXT")
  private String refundReason;

  @Column(name = "stripe_payment_intent_id")
  private String stripePaymentIntentId;
}
