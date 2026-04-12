package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductService;
import com.devikapps.caverne.modules.payment.PaymentProvider;
import com.devikapps.caverne.modules.payment.PaymentResponse;
import com.devikapps.caverne.modules.user.UserAccount;
import com.devikapps.caverne.modules.user.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;
  private final ProductService productService;
  private final DeliveryCostService deliveryCostService;
  private final List<PaymentProvider> paymentProviders;
  private final ObjectMapper objectMapper;
  private final OrderApiMapper orderApiMapper;

  @Transactional(readOnly = true)
  public Page<org.openapitools.client.model.Order> findAll(
      OrderStatus status, UUID userId, Pageable pageable) {
    return orderRepository
        .findAll(withStatus(status).and(withUserId(userId)), pageable)
        .map(orderApiMapper::toOrderModel);
  }

  @Transactional(readOnly = true)
  public Page<org.openapitools.client.model.Order> findAllForUser(
      UserAccount user, OrderStatus status, Pageable pageable) {
    return orderRepository
        .findAll(withOwner(user).and(withStatus(status)), pageable)
        .map(orderApiMapper::toOrderModel);
  }

  public org.openapitools.client.model.Order createOrder(
      org.openapitools.client.model.OrderInput input, UserAccount owner) {
    validateOrderInput(input);

    Order order =
        Order.builder()
            .reference("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .date(LocalDateTime.now())
            .status(OrderStatus.PENDING)
            .currencyCode(input.getCurrencyCode())
            .deliveryCostId(input.getDeliveryCostId())
            .recipientName(input.getRecipient().getRecipientName())
            .recipientEmail(input.getRecipient().getRecipientEmail())
            .recipientPhone(input.getRecipient().getRecipientPhone())
            .shippingLocation(input.getRecipient().getLocation())
            .postalCode(input.getRecipient().getPostalCode())
            .countryCode(input.getRecipient().getCountryCode())
            .user(owner)
            .totalAmount(BigDecimal.ZERO)
            .build();

    BigDecimal deliveryAmount = resolveDeliveryAmount(input);

    List<OrderItem> items =
        input.getItems().stream()
            .map(
                itemReq -> {
                  Product product = productService.findById(itemReq.getProductId());
                  BigDecimal unitPrice =
                      product.getPrices().stream()
                          .filter(
                              price ->
                                  input.getCurrencyCode().equalsIgnoreCase(price.getCurrencyCode()))
                          .max(java.util.Comparator.comparing(price -> price.getValidFrom()))
                          .map(price -> price.getValue())
                          .orElseThrow(
                              () ->
                                  new ResponseStatusException(
                                      UNPROCESSABLE_ENTITY,
                                      "No price available for product "
                                          + product.getId()
                                          + " in currency "
                                          + input.getCurrencyCode()));
                  return OrderItem.builder()
                      .order(order)
                      .productId(product.getId())
                      .productLabel(product.getLabel())
                      .quantity(BigDecimal.valueOf(itemReq.getQuantity()))
                      .unitPrice(unitPrice)
                      .totalPrice(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                      .build();
                })
            .toList();

    order.setItems(items);
    order.setTotalAmount(
        items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(deliveryAmount));

    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  public List<org.openapitools.client.model.Payment> listPayments(UUID orderId, UserAccount actor) {
    Order order = requireOrderAccess(findOrder(orderId), actor);
    return order.getPayments().stream()
        .map(payment -> orderApiMapper.toPaymentModel(order, payment))
        .toList();
  }

  public org.openapitools.client.model.Payment processPayment(
      UUID orderId, org.openapitools.client.model.PaymentInput input, UserAccount actor) {
    validatePaymentInput(input);
    Order order = requireOrderAccess(findOrder(orderId), actor);

    PaymentProvider provider =
        paymentProviders.stream()
            .filter(p -> p.getProviderCode().equalsIgnoreCase(input.getMethodCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        UNPROCESSABLE_ENTITY, "Payment provider not found"));

    PaymentResponse response =
        provider.initiatePayment(
            resolvePaymentAmount(order, input), input.getCurrencyCode(), order.getReference());

    order
        .getPayments()
        .add(
            OrderPayment.builder()
                .paymentId(UUID.randomUUID())
                .methodCode(input.getMethodCode())
                .currencyCode(input.getCurrencyCode())
                .amount(resolvePaymentAmount(order, input))
                .date(LocalDateTime.now())
                .status(response.status())
                .internalReference(response.transactionId())
                .providerResponse(writeProviderResponse(response.providerData()))
                .build());
    orderRepository.save(order);
    return orderApiMapper.toPaymentModel(order, order.getPayments().getLast());
  }

  public org.openapitools.client.model.Order getOrder(UUID id) {
    return orderApiMapper.toOrderModel(findOrder(id));
  }

  @Transactional(readOnly = true)
  public org.openapitools.client.model.Order getOrderForActor(UUID id, UserAccount actor) {
    return orderApiMapper.toOrderModel(requireOrderAccess(findOrder(id), actor));
  }

  @Transactional
  public org.openapitools.client.model.Order updateStatus(UUID id, OrderStatus status) {
    Order order = findOrder(id);
    order.setStatus(status);
    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  @Transactional
  public org.openapitools.client.model.Order cancelOrder(UUID id, UserAccount actor) {
    Order order = requireOrderAccess(findOrder(id), actor);
    order.setStatus(OrderStatus.CANCELLED);
    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  private Order findOrder(UUID id) {
    return orderRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
  }

  private Specification<Order> withStatus(OrderStatus status) {
    return (root, query, builder) ->
        status == null ? null : builder.equal(root.get("status"), status);
  }

  private Specification<Order> withOwner(UserAccount user) {
    return (root, query, builder) -> builder.equal(root.get("user").get("id"), user.getId());
  }

  private Specification<Order> withUserId(UUID userId) {
    return (root, query, builder) ->
        userId == null ? null : builder.equal(root.get("user").get("id"), userId);
  }

  private Order requireOrderAccess(Order order, UserAccount actor) {
    if (order.getUser() == null) {
      if (actor == null) {
        return order;
      }
      if (actor.getRole() == UserRole.ADMIN) {
        return order;
      }
      throw new ResponseStatusException(FORBIDDEN, "Access to this order is forbidden");
    }
    if (actor == null) {
      throw new ResponseStatusException(FORBIDDEN, "Authentication required for this order");
    }
    if (actor.getRole() == UserRole.ADMIN || order.getUser().getId().equals(actor.getId())) {
      return order;
    }
    throw new ResponseStatusException(FORBIDDEN, "Access to this order is forbidden");
  }

  private void validateOrderInput(org.openapitools.client.model.OrderInput input) {
    if (input == null || input.getCurrencyCode() == null || input.getCurrencyCode().isBlank()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "currency_code is required");
    }
    if (input.getItems() == null || input.getItems().isEmpty()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "items is required");
    }
    if (input.getRecipient() == null) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "recipient is required");
    }
    if (isBlank(input.getRecipient().getLocation())
        || isBlank(input.getRecipient().getPostalCode())
        || isBlank(input.getRecipient().getCountryCode())
        || isBlank(input.getRecipient().getRecipientName())
        || isBlank(input.getRecipient().getRecipientEmail())
        || isBlank(input.getRecipient().getRecipientPhone())) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "recipient is incomplete");
    }
    boolean invalidItem =
        input.getItems().stream()
            .anyMatch(
                item ->
                    item.getProductId() == null
                        || item.getQuantity() == null
                        || item.getQuantity() <= 0);
    if (invalidItem) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "items contain invalid values");
    }
  }

  private BigDecimal resolveDeliveryAmount(org.openapitools.client.model.OrderInput input) {
    if (input.getDeliveryCostId() == null) {
      return BigDecimal.ZERO;
    }
    DeliveryCost deliveryCost = deliveryCostService.requireEntityById(input.getDeliveryCostId());
    return deliveryCost.getAmount();
  }

  private void validatePaymentInput(org.openapitools.client.model.PaymentInput input) {
    if (input == null
        || isBlank(input.getMethodCode())
        || isBlank(input.getCurrencyCode())
        || input.getAmount() == null
        || input.getAmount() <= 0) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "payment input is invalid");
    }
  }

  private BigDecimal resolvePaymentAmount(Order order, org.openapitools.client.model.PaymentInput input) {
    BigDecimal totalAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    BigDecimal requestedAmount = BigDecimal.valueOf(input.getAmount());
    if (requestedAmount.compareTo(totalAmount) != 0) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "payment amount must match the order total amount");
    }
    return totalAmount;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String writeProviderResponse(Map<String, Object> providerData) {
    try {
      return objectMapper.writeValueAsString(providerData);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to serialize provider response");
    }
  }
}
