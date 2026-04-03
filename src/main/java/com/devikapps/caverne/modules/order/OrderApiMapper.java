package com.devikapps.caverne.modules.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderApiMapper {

  private final ObjectMapper objectMapper;

  public org.openapitools.client.model.Order toOrderModel(Order order) {
    return new org.openapitools.client.model.Order()
        .id(order.getId().intValue())
        .userId(order.getUser() == null ? null : order.getUser().getId().intValue())
        .currencyCode(order.getCurrencyCode())
        .reference(order.getReference())
        .date(order.getDate().atOffset(OffsetDateTime.now().getOffset()))
        .status(
            org.openapitools.client.model.Order.StatusEnum.fromValue(
                order.getStatus().name().toLowerCase()))
        .deliveryCostId(
            order.getDeliveryCostId() == null ? null : order.getDeliveryCostId().intValue())
        .items(
            order.getItems().stream()
                .map(
                    item ->
                        new org.openapitools.client.model.OrderItem()
                            .productId(item.getProductId().intValue())
                            .quantity(item.getQuantity().doubleValue())
                            .unitPrice(item.getUnitPrice().doubleValue()))
                .toList())
        .recipient(
            new org.openapitools.client.model.RecipientInput()
                .location(order.getShippingLocation())
                .postalCode(order.getPostalCode())
                .countryCode(order.getCountryCode())
                .recipientName(order.getRecipientName())
                .recipientEmail(order.getRecipientEmail())
                .recipientPhone(order.getRecipientPhone()));
  }

  public org.openapitools.client.model.Payment toPaymentModel(Order order, OrderPayment payment) {
    return new org.openapitools.client.model.Payment()
        .id(payment.getPaymentId())
        .orderId(order.getId().intValue())
        .methodCode(payment.getMethodCode())
        .currencyCode(payment.getCurrencyCode())
        .amount(payment.getAmount().doubleValue())
        .date(payment.getDate().atOffset(OffsetDateTime.now().getOffset()))
        .status(
            org.openapitools.client.model.Payment.StatusEnum.fromValue(
                payment.getStatus().toLowerCase()))
        .internalReference(payment.getInternalReference())
        .providerResponse(readProviderResponse(payment.getProviderResponse()));
  }

  private Map<String, Object> readProviderResponse(String rawProviderResponse) {
    if (rawProviderResponse == null || rawProviderResponse.isBlank()) {
      return Map.of();
    }

    try {
      return objectMapper.readValue(rawProviderResponse, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to read provider response");
    }
  }
}
