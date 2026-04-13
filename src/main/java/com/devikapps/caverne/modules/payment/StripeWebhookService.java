package com.devikapps.caverne.modules.payment;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderPayment;
import com.devikapps.caverne.modules.order.OrderPaymentRepository;
import com.devikapps.caverne.modules.order.OrderRepository;
import com.devikapps.caverne.modules.order.OrderStatus;
import com.devikapps.caverne.payment.StripeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripeWebhookService {

  private final StripeProperties stripeProperties;
  private final OrderRepository orderRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void handleWebhook(String payload, String signatureHeader) {
    if (stripeProperties.getWebhookSecret() == null
        || stripeProperties.getWebhookSecret().isBlank()) {
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "Stripe webhook secret is not configured");
    }
    try {
      Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
      JsonNode root = objectMapper.readTree(payload);
      String eventType = requiredText(root.path("type"), "Stripe event type is missing");
      JsonNode objectNode = root.path("data").path("object");
      String sessionId =
          requiredText(objectNode.path("id"), "Stripe checkout session id is missing");

      OrderPayment payment =
          orderPaymentRepository
              .findByInternalReference(sessionId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          org.springframework.http.HttpStatus.NOT_FOUND,
                          "Stripe payment not found"));

      Order order = payment.getOrder();
      updatePaymentFromEvent(payment, eventType, objectNode);
      orderPaymentRepository.save(payment);
      updateOrderFromPayment(order, payment);
      orderRepository.save(order);
    } catch (SignatureVerificationException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid Stripe signature");
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid Stripe webhook payload");
    }
  }

  private void updatePaymentFromEvent(OrderPayment payment, String eventType, JsonNode objectNode)
      throws Exception {
    String normalizedStatus = normalizeEventType(eventType);
    payment.setStatus(normalizedStatus);

    Map<String, Object> providerResponse = readProviderResponse(payment.getProviderResponse());
    putIfText(providerResponse, "checkout_session_id", objectNode.path("id"));
    putIfText(providerResponse, "checkout_status", objectNode.path("status"));
    putIfText(providerResponse, "payment_status", objectNode.path("payment_status"));
    providerResponse.put("last_webhook_event", eventType);
    providerResponse.put("webhook_processed_at", OffsetDateTime.now().toString());
    payment.setProviderResponse(objectMapper.writeValueAsString(providerResponse));
  }

  private void updateOrderFromPayment(Order order, OrderPayment payment) {
    if ("confirmed".equalsIgnoreCase(payment.getStatus())
        && order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CONFIRMED);
    }
  }

  private String normalizeEventType(String eventType) {
    return switch (eventType) {
      case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> "confirmed";
      case "checkout.session.async_payment_failed", "checkout.session.expired" -> "failed";
      default -> "pending";
    };
  }

  private Map<String, Object> readProviderResponse(String rawProviderResponse) throws Exception {
    if (rawProviderResponse == null || rawProviderResponse.isBlank()) {
      return new LinkedHashMap<>();
    }
    return objectMapper.readValue(rawProviderResponse, new TypeReference<>() {});
  }

  private void putIfText(Map<String, Object> target, String key, JsonNode node) {
    if (node != null && node.isTextual() && !node.asText().isBlank()) {
      target.put(key, node.asText());
    }
  }

  private String requiredText(JsonNode node, String message) {
    if (node == null || !node.isTextual() || node.asText().isBlank()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, message);
    }
    return node.asText();
  }
}
