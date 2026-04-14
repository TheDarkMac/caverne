package com.devikapps.caverne.modules.payment;

import com.stripe.exception.StripeException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalStripeExceptionHandler {

  @ExceptionHandler(StripeException.class)
  public ResponseEntity<Map<String, Object>> handleStripe(StripeException exception) {
    log.error("Stripe API error: code={} message={}", exception.getCode(), exception.getMessage(),
        exception);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(Map.of("error", "payment provider error"));
  }

  @ExceptionHandler(PaymentProviderException.class)
  public ResponseEntity<Map<String, Object>> handlePaymentProvider(
      PaymentProviderException exception) {
    log.error("Payment provider failure: {}", exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(Map.of("error", "payment provider error"));
  }
}
