package com.devikapps.caverne.modules.payment;

import java.math.BigDecimal;
import java.util.Map;

public class MockStripePaymentProvider implements PaymentProvider {

  @Override
  public String getProviderCode() {
    return "STRIPE";
  }

  @Override
  public PaymentResponse initiatePayment(
      BigDecimal amount, String currency, String orderReference) {
    return new PaymentResponse(
        "MOCK-" + orderReference,
        "pending",
        "https://checkout.stripe.test/session/mock-" + orderReference,
        Map.of(
            "checkout_url", "https://checkout.stripe.test/session/mock-" + orderReference,
            "checkout_session_id", "cs_test_mock_" + orderReference,
            "checkout_status", "open",
            "payment_status", "unpaid",
            "order_reference", orderReference,
            "currency", currency,
            "amount", amount));
  }
}
