package com.devikapps.caverne.modules.payment;

import java.math.BigDecimal;

public interface PaymentProvider {
  String getProviderCode();

  PaymentResponse initiatePayment(BigDecimal amount, String currency, String orderReference);
}
