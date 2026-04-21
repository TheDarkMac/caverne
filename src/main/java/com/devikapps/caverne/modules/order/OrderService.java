package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import com.devikapps.caverne.modules.catalog.Price;
import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductApiMapper;
import com.devikapps.caverne.modules.catalog.ProductRepository;
import com.devikapps.caverne.modules.catalog.StockMovement;
import com.devikapps.caverne.modules.catalog.StockMovementService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final ProductApiMapper productApiMapper;
  private final DeliveryCostService deliveryCostService;
  private final List<PaymentProvider> paymentProviders;
  private final ObjectMapper objectMapper;
  private final OrderApiMapper orderApiMapper;
  private final StockMovementService stockMovementService;

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
                  Product product =
                      productRepository
                          .findByIdForUpdate(itemReq.getProductId())
                          .orElseThrow(
                              () -> new ResponseStatusException(NOT_FOUND, "Product not found"));
                  BigDecimal quantity = BigDecimal.valueOf(itemReq.getQuantity());
                  ensureSufficientStock(product, quantity);
                  Price price =
                      resolveCurrentPrice(product, input.getCurrencyCode(), order.getDate());
                  BigDecimal unitPrice = price.getValue();
                  org.openapitools.client.model.Product productSnapshot =
                      productApiMapper.toOrderSnapshot(product, price);
                  BigDecimal newBalance = product.getStockQuantity().subtract(quantity);
                  product.setStockQuantity(newBalance);
                  stockMovementService.record(
                      product.getId(),
                      newBalance,
                      quantity.negate(),
                      StockMovement.Reason.ORDER_PLACED,
                      owner == null ? null : owner.getId(),
                      "Order " + order.getReference());
                  return OrderItem.builder()
                      .order(order)
                      .productId(product.getId())
                      .productLabel(product.getLabel())
                      .quantity(quantity)
                      .unitPrice(unitPrice)
                      .totalPrice(unitPrice.multiply(quantity))
                      .productSnapshot(writeProductSnapshot(productSnapshot))
                      .build();
                })
            .toList();

    order.setItems(items);
    order.setTotalAmount(
        items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(deliveryAmount));

    Order saved = orderRepository.save(order);
    log.info(
        "Order created reference={} id={} totalAmount={} currency={} itemCount={} userId={}",
        saved.getReference(),
        saved.getId(),
        saved.getTotalAmount(),
        saved.getCurrencyCode(),
        items.size(),
        owner == null ? null : owner.getId());
    return orderApiMapper.toOrderModel(saved);
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
                        UNPROCESSABLE_CONTENT, "Payment provider not found"));

    PaymentResponse response =
        provider.initiatePayment(
            order, resolvePaymentAmount(order, input), input.getCurrencyCode());

    order
        .getPayments()
        .add(
            OrderPayment.builder()
                .paymentId(UUID.randomUUID())
                .order(order)
                .methodCode(input.getMethodCode())
                .currencyCode(input.getCurrencyCode())
                .amount(resolvePaymentAmount(order, input))
                .date(LocalDateTime.now())
                .status(response.status())
                .internalReference(response.transactionId())
                .providerResponse(writeProviderResponse(response.providerData()))
                .build());
    orderRepository.save(order);
    OrderPayment persisted = order.getPayments().getLast();
    log.info(
        "Payment initiated orderId={} paymentId={} provider={} status={} amount={}",
        order.getId(),
        persisted.getPaymentId(),
        persisted.getMethodCode(),
        persisted.getStatus(),
        persisted.getAmount());
    return orderApiMapper.toPaymentModel(order, persisted);
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
    OrderStatus previous = order.getStatus();
    order.setStatus(status);
    log.info(
        "Order status updated orderId={} reference={} from={} to={}",
        order.getId(),
        order.getReference(),
        previous,
        status);
    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  @Transactional
  public org.openapitools.client.model.Order cancelOrder(UUID id, UserAccount actor) {
    Order order = requireOrderAccess(findOrder(id), actor);
    if (order.getStatus() != OrderStatus.CANCELLED) {
      restoreStockForOrder(order, actor);
    }
    order.setStatus(OrderStatus.CANCELLED);
    log.info(
        "Order cancelled orderId={} reference={} actorId={}",
        order.getId(),
        order.getReference(),
        actor == null ? null : actor.getId());
    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  private void restoreStockForOrder(Order order, UserAccount actor) {
    if (order.getItems() == null) {
      return;
    }
    for (OrderItem item : order.getItems()) {
      if (item.getProductId() == null || item.getQuantity() == null) {
        continue;
      }
      Product product = productRepository.findByIdForUpdate(item.getProductId()).orElse(null);
      if (product == null) {
        continue;
      }
      BigDecimal current =
          product.getStockQuantity() == null ? BigDecimal.ZERO : product.getStockQuantity();
      BigDecimal newBalance = current.add(item.getQuantity());
      product.setStockQuantity(newBalance);
      productRepository.save(product);
      stockMovementService.record(
          product.getId(),
          newBalance,
          item.getQuantity(),
          StockMovement.Reason.ORDER_CANCELLED,
          actor == null ? null : actor.getId(),
          "Order " + order.getReference() + " cancelled");
    }
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
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "currency_code is required");
    }
    if (input.getItems() == null || input.getItems().isEmpty()) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "items is required");
    }
    if (input.getRecipient() == null) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "recipient is required");
    }
    if (isBlank(input.getRecipient().getLocation())
        || isBlank(input.getRecipient().getPostalCode())
        || isBlank(input.getRecipient().getCountryCode())
        || isBlank(input.getRecipient().getRecipientName())
        || isBlank(input.getRecipient().getRecipientEmail())
        || isBlank(input.getRecipient().getRecipientPhone())) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "recipient is incomplete");
    }
    boolean invalidItem =
        input.getItems().stream()
            .anyMatch(
                item ->
                    item.getProductId() == null
                        || item.getQuantity() == null
                        || item.getQuantity() <= 0);
    if (invalidItem) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "items contain invalid values");
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
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "payment input is invalid");
    }
  }

  private BigDecimal resolvePaymentAmount(
      Order order, org.openapitools.client.model.PaymentInput input) {
    BigDecimal totalAmount =
        order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    BigDecimal requestedAmount = BigDecimal.valueOf(input.getAmount());
    if (requestedAmount.compareTo(totalAmount) != 0) {
      throw new ResponseStatusException(
          UNPROCESSABLE_CONTENT, "payment amount must match the order total amount");
    }
    return totalAmount;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Price resolveCurrentPrice(Product product, String currencyCode, LocalDateTime at) {
    return product.getPrices().stream()
        .filter(price -> currencyCode.equalsIgnoreCase(price.getCurrencyCode()))
        .max((left, right) -> comparePriceRecency(left, right, at.toLocalDate()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    UNPROCESSABLE_CONTENT,
                    "No price available for product "
                        + product.getId()
                        + " in currency "
                        + currencyCode));
  }

  private int comparePriceRecency(Price left, Price right, java.time.LocalDate effectiveDate) {
    java.time.LocalDate leftDate = normalizeApplicableDate(left.getValidFrom(), effectiveDate);
    java.time.LocalDate rightDate = normalizeApplicableDate(right.getValidFrom(), effectiveDate);
    return leftDate.compareTo(rightDate);
  }

  private java.time.LocalDate normalizeApplicableDate(
      java.time.LocalDate validFrom, java.time.LocalDate effectiveDate) {
    if (validFrom == null) {
      return java.time.LocalDate.MIN;
    }
    return validFrom.isAfter(effectiveDate) ? java.time.LocalDate.MIN : validFrom;
  }

  private void ensureSufficientStock(Product product, BigDecimal requestedQuantity) {
    BigDecimal stockQuantity =
        product.getStockQuantity() == null ? BigDecimal.ZERO : product.getStockQuantity();
    if (requestedQuantity.compareTo(stockQuantity) > 0) {
      throw new ResponseStatusException(
          UNPROCESSABLE_CONTENT,
          "Requested quantity exceeds available stock for product " + product.getId());
    }
  }

  private String writeProductSnapshot(org.openapitools.client.model.Product productSnapshot) {
    try {
      return productSnapshot.toJson();
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Unable to serialize product snapshot");
    }
  }

  private String writeProviderResponse(Map<String, Object> providerData) {
    try {
      return objectMapper.writeValueAsString(providerData);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to serialize provider response");
    }
  }
}
