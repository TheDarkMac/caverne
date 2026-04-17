package com.devikapps.caverne.modules.payment;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderPayment;
import com.devikapps.caverne.modules.order.OrderPaymentRepository;
import com.devikapps.caverne.modules.order.OrderStatus;
import com.devikapps.caverne.payment.StripeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripeWebhookService {

  private static final Set<String> HANDLED_EVENTS =
      Set.of(
          "checkout.session.completed",
          "checkout.session.async_payment_succeeded",
          "checkout.session.async_payment_failed",
          "checkout.session.expired",
          "payment_intent.succeeded",
          "payment_intent.payment_failed",
          "payment_intent.canceled",
          "charge.refunded",
          "charge.dispute.created");

  private static final Map<String, Integer> STATUS_PRECEDENCE =
      Map.of(
          "pending", 0,
          "failed", 1,
          "cancelled", 1,
          "confirmed", 2,
          "refunded", 3,
          "disputed", 3);

  private final StripeProperties stripeProperties;
  private final OrderPaymentRepository orderPaymentRepository;
  private final StripeWebhookEventRepository stripeWebhookEventRepository;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;
  private final Clock clock;

  public void handleWebhook(String payload, String signatureHeader) {
    String secret = stripeProperties.getWebhookSecret();
    if (secret == null || secret.isBlank()) {
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "Stripe webhook secret is not configured");
    }
    if (signatureHeader == null || signatureHeader.isBlank()) {
      throw new ResponseStatusException(BAD_REQUEST, "Missing Stripe-Signature header");
    }

    try {
      Webhook.constructEvent(payload, signatureHeader, secret);
    } catch (SignatureVerificationException exception) {
      log.warn("Invalid Stripe webhook signature: {}", exception.getMessage());
      throw new ResponseStatusException(BAD_REQUEST, "Invalid Stripe signature");
    }

    JsonNode root;
    String eventId;
    String eventType;
    JsonNode objectNode;
    try {
      root = objectMapper.readTree(payload);
      eventId = requiredText(root.path("id"), "Stripe event id is missing");
      eventType = requiredText(root.path("type"), "Stripe event type is missing");
      objectNode = root.path("data").path("object");
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      log.warn("Invalid Stripe webhook payload: {}", exception.getMessage());
      throw new ResponseStatusException(BAD_REQUEST, "Invalid Stripe webhook payload");
    }

    log.info("Received Stripe webhook event {} ({})", eventId, eventType);

    if (!HANDLED_EVENTS.contains(eventType)) {
      log.debug("Ignoring unhandled Stripe event type {}", eventType);
      return;
    }

    final String finalEventId = eventId;
    final String finalEventType = eventType;
    final JsonNode finalObject = objectNode;
    transactionTemplate.executeWithoutResult(
        status -> handleVerifiedEvent(finalEventId, finalEventType, finalObject));
  }

  private void handleVerifiedEvent(String eventId, String eventType, JsonNode objectNode) {
    if (stripeWebhookEventRepository.existsById(eventId)) {
      log.info("Skipping duplicate Stripe webhook event {}", eventId);
      return;
    }
    try {
      stripeWebhookEventRepository.saveAndFlush(
          StripeWebhookEvent.builder()
              .eventId(eventId)
              .receivedAt(OffsetDateTime.now(clock))
              .build());
    } catch (DataIntegrityViolationException duplicate) {
      log.info("Duplicate Stripe webhook event {} detected on insert", eventId);
      return;
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
      return;
    }
    OrderPayment payment = paymentOpt.get();
    Order order = payment.getOrder();
    try {
      updatePaymentFromEvent(payment, eventType, objectNode);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    updateOrderFromPayment(order, payment);
  }

  private Optional<OrderPayment> lookupPayment(String eventType, JsonNode objectNode) {
    if (eventType.startsWith("payment_intent.")) {
      String paymentIntentId =
          requiredText(objectNode.path("id"), "Stripe payment intent id is missing");
      return orderPaymentRepository
          .findByStripePaymentIntentId(paymentIntentId)
          .or(() -> orderPaymentRepository.findByInternalReference(paymentIntentId));
    }
    if (eventType.startsWith("charge.")) {
      String paymentIntentId = textOrNull(objectNode.path("payment_intent"));
      if (paymentIntentId != null) {
        Optional<OrderPayment> byIntent =
            orderPaymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (byIntent.isPresent()) {
          return byIntent;
        }
      }
      String chargeId = textOrNull(objectNode.path("id"));
      if (chargeId != null) {
        return orderPaymentRepository.findByInternalReference(chargeId);
      }
      return Optional.empty();
    }
    String sessionId = requiredText(objectNode.path("id"), "Stripe checkout session id is missing");
    return orderPaymentRepository.findByInternalReference(sessionId);
  }

  private void updatePaymentFromEvent(OrderPayment payment, String eventType, JsonNode objectNode)
      throws Exception {
    String targetStatus = normalizeEventType(eventType);
    if (shouldApplyStatus(payment.getStatus(), targetStatus)) {
      payment.setStatus(targetStatus);
    } else {
      log.info(
          "Keeping payment {} status {} (received event {} → {})",
          payment.getPaymentId(),
          payment.getStatus(),
          eventType,
          targetStatus);
    }

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

    if ("charge.refunded".equals(eventType)) {
      applyRefundDetails(payment, objectNode);
    }

    Map<String, Object> providerResponse = readProviderResponse(payment.getProviderResponse());
    putIfText(providerResponse, "checkout_session_id", objectNode.path("id"));
    putIfText(providerResponse, "checkout_status", objectNode.path("status"));
    putIfText(providerResponse, "payment_status", objectNode.path("payment_status"));
    putIfText(providerResponse, "payment_intent_id", objectNode.path("payment_intent"));
    providerResponse.put("last_webhook_event", eventType);
    providerResponse.put("webhook_processed_at", OffsetDateTime.now(clock).toString());
    payment.setProviderResponse(objectMapper.writeValueAsString(providerResponse));
  }

  private void applyRefundDetails(OrderPayment payment, JsonNode objectNode) {
    String chargeId = textOrNull(objectNode.path("id"));
    if (chargeId != null && (payment.getRefundId() == null || payment.getRefundId().isBlank())) {
      payment.setRefundId(chargeId);
    }
    JsonNode refundedAmount = objectNode.path("amount_refunded");
    if (refundedAmount.isNumber()) {
      payment.setRefundedAmount(refundedAmount.decimalValue());
    }
    if (payment.getRefundedAt() == null) {
      payment.setRefundedAt(OffsetDateTime.now(clock));
    }
  }

  private void updateOrderFromPayment(Order order, OrderPayment payment) {
    if ("confirmed".equalsIgnoreCase(payment.getStatus())
        && order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CONFIRMED);
    }
  }

  private boolean shouldApplyStatus(String current, String target) {
    if (target == null) {
      return false;
    }
    if (current == null || current.isBlank()) {
      return true;
    }
    Integer currentP = STATUS_PRECEDENCE.get(current.toLowerCase());
    Integer targetP = STATUS_PRECEDENCE.get(target.toLowerCase());
    if (currentP == null) {
      return true;
    }
    if (targetP == null) {
      return false;
    }
    return targetP >= currentP;
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
      case "payment_intent.canceled" -> "cancelled";
      case "charge.refunded" -> "refunded";
      case "charge.dispute.created" -> "disputed";
      default -> null;
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

  private String textOrNull(JsonNode node) {
    if (node == null || !node.isTextual() || node.asText().isBlank()) {
      return null;
    }
    return node.asText();
  }

  private String requiredText(JsonNode node, String message) {
    if (node == null || !node.isTextual() || node.asText().isBlank()) {
      throw new ResponseStatusException(BAD_REQUEST, message);
    }
    return node.asText();
  }
}
