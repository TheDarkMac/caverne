package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductService;
import com.devikapps.caverne.modules.payment.PaymentProvider;
import com.devikapps.caverne.modules.payment.PaymentResponse;
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
  private final List<PaymentProvider> paymentProviders;
  private final ObjectMapper objectMapper;
  private final OrderApiMapper orderApiMapper;

  @Transactional(readOnly = true)
  public Page<org.openapitools.client.model.Order> findAll(OrderStatus status, Pageable pageable) {
    return orderRepository.findAll(withStatus(status), pageable).map(orderApiMapper::toOrderModel);
  }

  public org.openapitools.client.model.Order createOrder(
      org.openapitools.client.model.OrderInput input) {
    validateOrderInput(input);

    Order order =
        Order.builder()
            .reference("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .date(LocalDateTime.now())
            .status(OrderStatus.PENDING)
            .currencyCode(input.getCurrencyCode())
            .customerName(input.getGuestAddress().getCustomerName())
            .customerEmail(input.getGuestAddress().getCustomerEmail())
            .customerPhone(input.getGuestAddress().getCustomerPhone())
            .shippingLocation(input.getGuestAddress().getLocation())
            .postalCode(input.getGuestAddress().getPostalCode())
            .countryCode(input.getGuestAddress().getCountryCode())
            .totalAmount(BigDecimal.ZERO)
            .build();

    List<OrderItem> items =
        input.getItems().stream()
            .map(
                itemReq -> {
                  Product product = productService.findById(itemReq.getProductId().longValue());
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
        items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add));

    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  public List<org.openapitools.client.model.Payment> listPayments(Long orderId) {
    Order order = findOrder(orderId);
    return order.getPayments().stream()
        .map(payment -> orderApiMapper.toPaymentModel(order, payment))
        .toList();
  }

  public org.openapitools.client.model.Payment processPayment(
      Long orderId, org.openapitools.client.model.PaymentInput input) {
    validatePaymentInput(input);
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

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
            BigDecimal.valueOf(input.getAmount()), input.getCurrencyCode(), order.getReference());

    int nextPaymentId =
        order.getPayments().stream()
                .map(OrderPayment::getPaymentId)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0)
            + 1;

    order
        .getPayments()
        .add(
            OrderPayment.builder()
                .paymentId(nextPaymentId)
                .methodCode(input.getMethodCode())
                .currencyCode(input.getCurrencyCode())
                .amount(BigDecimal.valueOf(input.getAmount()))
                .date(LocalDateTime.now())
                .status(response.status())
                .internalReference(response.transactionId())
                .providerResponse(writeProviderResponse(response.providerData()))
                .build());
    orderRepository.save(order);

    return orderApiMapper.toPaymentModel(order, order.getPayments().getLast());
  }

  public org.openapitools.client.model.Order getOrder(Long id) {
    return orderApiMapper.toOrderModel(findOrder(id));
  }

  @Transactional
  public org.openapitools.client.model.Order updateStatus(Long id, OrderStatus status) {
    Order order = findOrder(id);
    order.setStatus(status);
    return orderApiMapper.toOrderModel(orderRepository.save(order));
  }

  private Order findOrder(Long id) {
    return orderRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
  }

  private Specification<Order> withStatus(OrderStatus status) {
    return (root, query, builder) ->
        status == null ? null : builder.equal(root.get("status"), status);
  }

  private void validateOrderInput(org.openapitools.client.model.OrderInput input) {
    if (input == null || input.getCurrencyCode() == null || input.getCurrencyCode().isBlank()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "currency_code is required");
    }
    if (input.getItems() == null || input.getItems().isEmpty()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "items is required");
    }
    if (input.getGuestAddress() == null) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "guest_address is required");
    }
    if (isBlank(input.getGuestAddress().getLocation())
        || isBlank(input.getGuestAddress().getPostalCode())
        || isBlank(input.getGuestAddress().getCountryCode())
        || isBlank(input.getGuestAddress().getCustomerName())
        || isBlank(input.getGuestAddress().getCustomerEmail())
        || isBlank(input.getGuestAddress().getCustomerPhone())) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "guest_address is incomplete");
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

  private void validatePaymentInput(org.openapitools.client.model.PaymentInput input) {
    if (input == null
        || isBlank(input.getMethodCode())
        || isBlank(input.getCurrencyCode())
        || input.getAmount() == null
        || input.getAmount() <= 0) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "payment input is invalid");
    }
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
