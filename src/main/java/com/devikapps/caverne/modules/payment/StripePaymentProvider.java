package com.devikapps.caverne.modules.payment;

import com.devikapps.caverne.payment.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

  private final StripeProperties stripeProperties;

  @Override
  public String getProviderCode() {
    return "STRIPE";
  }

  @Override
  public PaymentResponse initiatePayment(
      BigDecimal amount, String currency, String orderReference) {
    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(convertToStripeAmount(amount))
            .setCurrency(currency.toLowerCase())
            .setDescription("Order " + orderReference)
            .putMetadata("order_reference", orderReference)
            .addPaymentMethodType("card")
            .build();

    RequestOptions.RequestOptionsBuilder builder =
        RequestOptions.builder().setIdempotencyKey("order-" + orderReference);
    RequestOptions requestOptions;
    if (stripeProperties.getApiVersion() != null && !stripeProperties.getApiVersion().isBlank()) {
      requestOptions =
          RequestOptions.RequestOptionsBuilder.unsafeSetStripeVersionOverride(
                  builder, stripeProperties.getApiVersion())
              .build();
    } else {
      requestOptions = builder.build();
    }

    try {
      PaymentIntent intent = PaymentIntent.create(params, requestOptions);
      Map<String, Object> providerData = new HashMap<>();
      providerData.put("client_secret", intent.getClientSecret());
      return new PaymentResponse(
          intent.getId(), intent.getStatus(), intent.getClientSecret(), providerData);
    } catch (StripeException exception) {
      throw new IllegalStateException("Stripe payment initiation failed", exception);
    }
  }

  private long convertToStripeAmount(BigDecimal amount) {
    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }
}
