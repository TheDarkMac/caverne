package com.devikapps.caverne.modules.payment;

import com.devikapps.caverne.payment.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

  private static final Set<String> ZERO_DECIMAL_CURRENCIES =
      Set.of(
          "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF", "UGX", "VND", "VUV",
          "XAF", "XOF", "XPF");

  private final StripeProperties stripeProperties;

  @Override
  public String getProviderCode() {
    return "STRIPE";
  }

  @Override
  public PaymentResponse initiatePayment(
      BigDecimal amount, String currency, String orderReference) {
    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(stripeProperties.getCheckoutSuccessUrl())
            .setCancelUrl(stripeProperties.getCheckoutCancelUrl())
            .setClientReferenceId(orderReference)
            .putMetadata("order_reference", orderReference)
            .addLineItem(buildLineItem(amount, currency, orderReference))
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
      Session session = Session.create(params, requestOptions);
      Map<String, Object> providerData = buildProviderData(session);
      return new PaymentResponse(
          session.getId(),
          coalesceStatus(session.getPaymentStatus(), session.getStatus()),
          session.getUrl(),
          providerData);
    } catch (StripeException exception) {
      throw new IllegalStateException("Stripe payment initiation failed", exception);
    }
  }

  private Map<String, Object> buildProviderData(Session session) {
    Map<String, Object> providerData = new HashMap<>();
    providerData.put("checkout_url", session.getUrl());
    providerData.put("checkout_session_id", session.getId());
    providerData.put("checkout_status", session.getStatus());
    providerData.put("payment_status", session.getPaymentStatus());
    return providerData;
  }

  private SessionCreateParams.LineItem buildLineItem(
      BigDecimal amount, String currency, String orderReference) {
    return SessionCreateParams.LineItem.builder()
        .setQuantity(1L)
        .setPriceData(
            SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(normalizeCurrency(currency))
                .setUnitAmount(convertToStripeAmount(amount, currency))
                .setProductData(
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Order " + orderReference)
                        .setDescription("Checkout for order " + orderReference)
                        .build())
                .build())
        .build();
  }

  private String coalesceStatus(String paymentStatus, String checkoutStatus) {
    if (paymentStatus != null && !paymentStatus.isBlank()) {
      return paymentStatus;
    }
    if (checkoutStatus != null && !checkoutStatus.isBlank()) {
      return checkoutStatus;
    }
    return "pending";
  }

  private long convertToStripeAmount(BigDecimal amount, String currency) {
    BigDecimal normalizedAmount =
        ZERO_DECIMAL_CURRENCIES.contains(normalizeCurrency(currency).toUpperCase(Locale.ROOT))
            ? amount
            : amount.movePointRight(2);
    return normalizedAmount.setScale(0, RoundingMode.HALF_UP).longValueExact();
  }

  private String normalizeCurrency(String currency) {
    return currency.trim().toLowerCase(Locale.ROOT);
  }
}
