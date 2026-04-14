package com.devikapps.caverne.modules.payment;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderPayment;
import com.devikapps.caverne.modules.order.OrderRepository;
import com.devikapps.caverne.modules.order.OrderStatus;
import com.devikapps.caverne.payment.StripeProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

  private static final Set<OrderStatus> CANCELLABLE_ON_REFUND =
      EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

  private final OrderRepository orderRepository;
  private final ObjectProvider<StripeRefundClient> stripeRefundClient;
  private final StripeProperties stripeProperties;

  @Transactional
  public Map<String, Object> refundOrder(UUID orderId, BigDecimal requestedAmount, String reason) {
    StripeProperties props = stripeProperties;
    if (props == null || !props.isEnabled()) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Stripe is disabled");
    }
    StripeRefundClient client = stripeRefundClient.getIfAvailable();
    if (client == null) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Stripe refund client is unavailable");
    }

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

    OrderPayment payment = pickRefundablePayment(order);
    BigDecimal captured = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
    BigDecimal alreadyRefunded =
        payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
    BigDecimal remaining = captured.subtract(alreadyRefunded);

    if (remaining.signum() <= 0) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Payment is already fully refunded");
    }

    BigDecimal amount = requestedAmount == null ? remaining : requestedAmount;
    if (amount.signum() <= 0) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Refund amount must be positive");
    }
    if (amount.compareTo(remaining) > 0) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "Refund amount exceeds captured amount");
    }

    StripeRefundClient.RefundResult result;
    try {
      result =
          client.refund(
              payment.getInternalReference(),
              amount,
              payment.getCurrencyCode(),
              reason,
              buildIdempotencyKey(order.getId(), payment.getPaymentId()));
    } catch (RuntimeException exception) {
      log.error(
          "Stripe refund failed for order {} payment {}: {}",
          order.getId(),
          payment.getPaymentId(),
          exception.getMessage(),
          exception);
      throw exception;
    }

    payment.setRefundId(result.refundId());
    payment.setRefundedAmount(alreadyRefunded.add(amount));
    payment.setRefundedAt(OffsetDateTime.now());
    payment.setRefundReason(reason);

    boolean fullyRefunded = payment.getRefundedAmount().compareTo(captured) >= 0;
    if (fullyRefunded) {
      payment.setStatus("refunded");
      if (CANCELLABLE_ON_REFUND.contains(order.getStatus())) {
        order.setStatus(OrderStatus.CANCELLED);
      } else {
        log.warn(
            "Refund processed for order {} but status {} is not cancellable; leaving order status unchanged",
            order.getId(),
            order.getStatus());
      }
    } else {
      payment.setStatus("partially_refunded");
    }
    orderRepository.save(order);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("order_id", order.getId());
    response.put("payment_id", payment.getPaymentId());
    response.put("refund_id", payment.getRefundId());
    response.put("refunded_amount", payment.getRefundedAmount());
    response.put("refund_status", result.status());
    response.put("payment_status", payment.getStatus());
    response.put("order_status", order.getStatus().name());
    return response;
  }

  private OrderPayment pickRefundablePayment(Order order) {
    List<OrderPayment> refundable =
        order.getPayments().stream()
            .filter(p -> "STRIPE".equalsIgnoreCase(p.getMethodCode()))
            .filter(
                p ->
                    p.getStatus() == null
                        || (!"failed".equalsIgnoreCase(p.getStatus())
                            && !"refunded".equalsIgnoreCase(p.getStatus())))
            .sorted(
                Comparator.comparing(
                        OrderPayment::getDate,
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                    .reversed()
                    .thenComparing(
                        OrderPayment::getPaymentId,
                        Comparator.nullsFirst(Comparator.<UUID>naturalOrder())))
            .toList();
    if (refundable.isEmpty()) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "No refundable Stripe payment for this order");
    }
    return refundable.get(0);
  }

  private String buildIdempotencyKey(UUID orderId, UUID paymentId) {
    return "refund-" + orderId + "-" + paymentId;
  }
}
