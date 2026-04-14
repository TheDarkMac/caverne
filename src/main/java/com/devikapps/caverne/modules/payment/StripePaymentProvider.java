package com.devikapps.caverne.modules.payment;

import com.devikapps.caverne.modules.order.DeliveryCost;
import com.devikapps.caverne.modules.order.DeliveryCostService;
import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderItem;
import com.devikapps.caverne.payment.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

  private static final Set<String> ZERO_DECIMAL_CURRENCIES =
      Set.of(
          "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF", "UGX", "VND", "VUV",
          "XAF", "XOF", "XPF");

  private final StripeProperties stripeProperties;
  private final DeliveryCostService deliveryCostService;

  @Override
  public String getProviderCode() {
    return "STRIPE";
  }

  @Override
  public PaymentResponse initiatePayment(
      BigDecimal amount, String currency, String orderReference) {
    SessionCreateParams.Builder params =
        baseParamsBuilder(orderReference)
            .addLineItem(buildSingleLineItem(amount, currency, orderReference));
    return createSession(params.build(), orderReference);
  }

  @Override
  public PaymentResponse initiatePayment(Order order, BigDecimal amount, String currency) {
    SessionCreateParams.Builder params = baseParamsBuilder(order.getReference());
    List<SessionCreateParams.LineItem> items = buildOrderLineItems(order, currency);
    if (items.isEmpty()) {
      params.addLineItem(buildSingleLineItem(amount, currency, order.getReference()));
    } else {
      items.forEach(params::addLineItem);
      SessionCreateParams.LineItem shipping = buildShippingLineItem(order, currency);
      if (shipping != null) {
        params.addLineItem(shipping);
      }
    }
    return createSession(params.build(), order.getReference());
  }

  private SessionCreateParams.Builder baseParamsBuilder(String orderReference) {
    return SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setSuccessUrl(stripeProperties.getCheckoutSuccessUrl())
        .setCancelUrl(stripeProperties.getCheckoutCancelUrl())
        .setClientReferenceId(orderReference)
        .putMetadata("order_reference", orderReference);
  }

  private PaymentResponse createSession(SessionCreateParams params, String orderReference) {
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
      log.error(
          "Stripe payment initiation failed for order {}: {}",
          orderReference,
          exception.getMessage(),
          exception);
      throw new PaymentProviderException("Stripe payment initiation failed", exception);
    }
  }

  private Map<String, Object> buildProviderData(Session session) {
    Map<String, Object> providerData = new HashMap<>();
    providerData.put("checkout_url", session.getUrl());
    providerData.put("checkout_session_id", session.getId());
    providerData.put("checkout_status", session.getStatus());
    providerData.put("payment_status", session.getPaymentStatus());
    if (session.getPaymentIntent() != null) {
      providerData.put("payment_intent_id", session.getPaymentIntent());
    }
    return providerData;
  }

  private SessionCreateParams.LineItem buildSingleLineItem(
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

  private List<SessionCreateParams.LineItem> buildOrderLineItems(Order order, String currency) {
    List<SessionCreateParams.LineItem> items = new ArrayList<>();
    if (order.getItems() == null) {
      return items;
    }
    for (OrderItem orderItem : order.getItems()) {
      if (orderItem.getUnitPrice() == null || orderItem.getQuantity() == null) {
        continue;
      }
      long quantity = orderItem.getQuantity().setScale(0, RoundingMode.HALF_UP).longValueExact();
      if (quantity <= 0) {
        continue;
      }
      String name =
          orderItem.getProductLabel() == null || orderItem.getProductLabel().isBlank()
              ? "Product " + orderItem.getProductId()
              : orderItem.getProductLabel();
      items.add(
          SessionCreateParams.LineItem.builder()
              .setQuantity(quantity)
              .setPriceData(
                  SessionCreateParams.LineItem.PriceData.builder()
                      .setCurrency(normalizeCurrency(currency))
                      .setUnitAmount(convertToStripeAmount(orderItem.getUnitPrice(), currency))
                      .setProductData(
                          SessionCreateParams.LineItem.PriceData.ProductData.builder()
                              .setName(name)
                              .build())
                      .build())
              .build());
    }
    return items;
  }

  private SessionCreateParams.LineItem buildShippingLineItem(Order order, String currency) {
    if (order.getDeliveryCostId() == null) {
      return null;
    }
    try {
      DeliveryCost cost = deliveryCostService.requireEntityById(order.getDeliveryCostId());
      if (cost == null || cost.getAmount() == null || cost.getAmount().signum() <= 0) {
        return null;
      }
      return SessionCreateParams.LineItem.builder()
          .setQuantity(1L)
          .setPriceData(
              SessionCreateParams.LineItem.PriceData.builder()
                  .setCurrency(normalizeCurrency(currency))
                  .setUnitAmount(convertToStripeAmount(cost.getAmount(), currency))
                  .setProductData(
                      SessionCreateParams.LineItem.PriceData.ProductData.builder()
                          .setName("Shipping")
                          .build())
                  .build())
          .build();
    } catch (RuntimeException exception) {
      log.warn("Could not resolve delivery cost for order {}", order.getReference(), exception);
      return null;
    }
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
