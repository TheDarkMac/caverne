package com.devikapps.caverne.modules.payment;

import java.math.BigDecimal;

public interface StripeRefundClient {

  /**
   * Initiates a refund against the given Stripe payment reference.
   *
   * @param paymentInternalReference the original Stripe payment intent or checkout session id
   * @param amount the amount to refund (in major currency units)
   * @param currency ISO 4217 currency code
   * @param reason optional reason
   * @return refund metadata
   */
  RefundResult refund(
      String paymentInternalReference, BigDecimal amount, String currency, String reason);

  /**
   * Initiates a refund with an idempotency key forwarded to Stripe. Default implementation
   * delegates to the legacy method so existing test doubles keep working.
   */
  default RefundResult refund(
      String paymentInternalReference,
      BigDecimal amount,
      String currency,
      String reason,
      String idempotencyKey) {
    return refund(paymentInternalReference, amount, currency, reason);
  }

  record RefundResult(String refundId, String status) {}
}
