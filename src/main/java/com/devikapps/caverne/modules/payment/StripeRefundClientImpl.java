package com.devikapps.caverne.modules.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripeRefundClientImpl implements StripeRefundClient {

  private static final Set<String> ZERO_DECIMAL_CURRENCIES =
      Set.of(
          "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF", "UGX", "VND", "VUV",
          "XAF", "XOF", "XPF");

  @Override
  public RefundResult refund(
      String paymentInternalReference, BigDecimal amount, String currency, String reason) {
    return refund(paymentInternalReference, amount, currency, reason, null);
  }

  @Override
  public RefundResult refund(
      String paymentInternalReference,
      BigDecimal amount,
      String currency,
      String reason,
      String idempotencyKey) {
    try {
      String paymentIntentId = resolvePaymentIntent(paymentInternalReference);
      RefundCreateParams.Builder builder =
          RefundCreateParams.builder().setPaymentIntent(paymentIntentId);
      if (amount != null) {
        builder.setAmount(toStripeAmount(amount, currency));
      }
      if (reason != null && !reason.isBlank()) {
        builder.putMetadata("reason", reason);
      }
      RequestOptions.RequestOptionsBuilder optionsBuilder = RequestOptions.builder();
      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        optionsBuilder.setIdempotencyKey(idempotencyKey);
      }
      Refund refund = Refund.create(builder.build(), optionsBuilder.build());
      return new RefundResult(refund.getId(), refund.getStatus());
    } catch (StripeException exception) {
      log.error(
          "Stripe refund failed for reference {}: code={} message={}",
          paymentInternalReference,
          exception.getCode(),
          exception.getMessage(),
          exception);
      throw new PaymentProviderException("Stripe refund failed", exception);
    }
  }

  private String resolvePaymentIntent(String reference) throws StripeException {
    if (reference == null) {
      throw new IllegalStateException("Missing payment reference");
    }
    if (reference.startsWith("cs_")) {
      Session session =
          Session.retrieve(
              reference, SessionRetrieveParams.builder().build(), RequestOptions.builder().build());
      String paymentIntent = session.getPaymentIntent();
      if (paymentIntent == null || paymentIntent.isBlank()) {
        throw new IllegalStateException("Stripe checkout session has no payment intent");
      }
      return paymentIntent;
    }
    return reference;
  }

  private long toStripeAmount(BigDecimal amount, String currency) {
    String normalized =
        currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    BigDecimal scaled =
        ZERO_DECIMAL_CURRENCIES.contains(normalized) ? amount : amount.movePointRight(2);
    return scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
  }
}
