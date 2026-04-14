package com.devikapps.caverne.modules.payment;

import com.devikapps.caverne.modules.order.Order;
import java.math.BigDecimal;

public interface PaymentProvider {
  String getProviderCode();

  PaymentResponse initiatePayment(BigDecimal amount, String currency, String orderReference);

  default PaymentResponse initiatePayment(Order order, BigDecimal amount, String currency) {
    return initiatePayment(amount, currency, order.getReference());
  }
}
