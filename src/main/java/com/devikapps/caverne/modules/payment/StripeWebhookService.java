package com.devikapps.caverne.modules.payment;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripeWebhookService {

  private final StripeProperties stripeProperties;
  private final OrderRepository orderRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final StripeWebhookEventRepository stripeWebhookEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void handleWebhook(String payload, String signatureHeader) {
    if (stripeProperties.getWebhookSecret() == null
        || stripeProperties.getWebhookSecret().isBlank()) {
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "Stripe webhook secret is not configured");
    }

    JsonNode root;
    String eventType;
    String eventId;
    JsonNode objectNode;
    try {
      Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
      root = objectMapper.readTree(payload);
      eventType = requiredText(root.path("type"), "Stripe event type is missing");
      eventId = requiredText(root.path("id"), "Stripe event id is missing");
      objectNode = root.path("data").path("object");
    } catch (SignatureVerificationException exception) {
      log.warn("Invalid Stripe webhook signature: {}", exception.getMessage());
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid Stripe signature");
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      log.warn("Invalid Stripe webhook payload: {}", exception.getMessage());
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid Stripe webhook payload");
    }

    // Idempotency guard
    try {
      if (stripeWebhookEventRepository.existsById(eventId)) {
        log.info("Skipping duplicate Stripe webhook event {}", eventId);
        return;
      }
      stripeWebhookEventRepository.saveAndFlush(
          StripeWebhookEvent.builder().eventId(eventId).receivedAt(OffsetDateTime.now()).build());
    } catch (DataIntegrityViolationException duplicate) {
      log.info("Duplicate Stripe webhook event {} detected on insert", eventId);
      return;
    } catch (RuntimeException exception) {
      log.error("Failed to record Stripe webhook event {}", eventId, exception);
      throw new ResponseStatusException(
          INTERNAL_SERVER_ERROR, "Failed to record Stripe webhook event");
    }

    try {
      processEvent(eventType, objectNode);
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.error("Failed to process Stripe webhook event {} ({})", eventId, eventType, exception);
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to process Stripe webhook");
    }
  }

  private void processEvent(String eventType, JsonNode objectNode) {
    Optional<OrderPayment> paymentOpt = lookupPayment(eventType, objectNode);
    if (paymentOpt.isEmpty()) {
      log.warn("No matching OrderPayment for Stripe event {}", eventType);
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Stripe payment not found");
    }
    OrderPayment payment = paymentOpt.get();
    Order order = payment.getOrder();
    try {
      updatePaymentFromEvent(payment, eventType, objectNode);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    orderPaymentRepository.save(payment);
    updateOrderFromPayment(order, payment);
    orderRepository.save(order);
  }

  private Optional<OrderPayment> lookupPayment(String eventType, JsonNode objectNode) {
    if (eventType.startsWith("payment_intent.")) {
      String paymentIntentId =
          requiredText(objectNode.path("id"), "Stripe payment intent id is missing");
      Optional<OrderPayment> byIntent =
          orderPaymentRepository.findByStripePaymentIntentId(paymentIntentId);
      if (byIntent.isPresent()) {
        return byIntent;
      }
      // Fallback to internal_reference if it happens to store PI id directly.
      return orderPaymentRepository.findByInternalReference(paymentIntentId);
    }
    String sessionId = requiredText(objectNode.path("id"), "Stripe checkout session id is missing");
    return orderPaymentRepository.findByInternalReference(sessionId);
  }

  private void updatePaymentFromEvent(OrderPayment payment, String eventType, JsonNode objectNode)
      throws Exception {
    String normalizedStatus = normalizeEventType(eventType);
    payment.setStatus(normalizedStatus);

    // Persist the payment_intent id when the session includes it.
    JsonNode piNode = objectNode.path("payment_intent");
    if (piNode.isTextual()
        && !piNode.asText().isBlank()
        && (payment.getStripePaymentIntentId() == null
            || payment.getStripePaymentIntentId().isBlank())) {
      payment.setStripePaymentIntentId(piNode.asText());
    }
    if (eventType.startsWith("payment_intent.")) {
      JsonNode idNode = objectNode.path("id");
      if (idNode.isTextual()
          && !idNode.asText().isBlank()
          && (payment.getStripePaymentIntentId() == null
              || payment.getStripePaymentIntentId().isBlank())) {
        payment.setStripePaymentIntentId(idNode.asText());
      }
    }

    Map<String, Object> providerResponse = readProviderResponse(payment.getProviderResponse());
    putIfText(providerResponse, "checkout_session_id", objectNode.path("id"));
    putIfText(providerResponse, "checkout_status", objectNode.path("status"));
    putIfText(providerResponse, "payment_status", objectNode.path("payment_status"));
    putIfText(providerResponse, "payment_intent_id", objectNode.path("payment_intent"));
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
      case "checkout.session.completed",
              "checkout.session.async_payment_succeeded",
              "payment_intent.succeeded" ->
          "confirmed";
      case "checkout.session.async_payment_failed",
              "checkout.session.expired",
              "payment_intent.payment_failed" ->
          "failed";
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
