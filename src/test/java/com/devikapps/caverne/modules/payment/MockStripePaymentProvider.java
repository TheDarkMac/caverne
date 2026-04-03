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
        null,
        Map.of(
            "client_secret", "mock-client-secret",
            "order_reference", orderReference,
            "currency", currency,
            "amount", amount));
  }
}
