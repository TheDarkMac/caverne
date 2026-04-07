package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.user.AuthSessionResolver;
import com.devikapps.caverne.modules.user.UserAccount;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final AuthSessionResolver authSessionResolver;

  @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listMyOrders(
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {
    UserAccount currentUser = authSessionResolver.requireUser(authorizationHeader);
    var p =
        orderService.findAllForUser(
            currentUser, parseOrderStatus(status), PageRequest.of(page - 1, per_page));
    org.openapitools.client.model.OrdersGet200Response response =
        new org.openapitools.client.model.OrdersGet200Response()
            .data(p.getContent())
            .meta(
                new org.openapitools.client.model.PaginatedMeta()
                    .total(Math.toIntExact(p.getTotalElements()))
                    .page(p.getNumber() + 1)
                    .perPage(p.getSize())
                    .lastPage(p.getTotalPages()));
    return JSON.getGson().toJson(response);
  }

  @GetMapping(value = "/orders/all", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listAllOrders(
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer user_id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {
    authSessionResolver.requireAdmin(authorizationHeader);

    var p =
        orderService.findAll(
            parseOrderStatus(status),
            user_id == null ? null : user_id.longValue(),
            PageRequest.of(page - 1, per_page));
    org.openapitools.client.model.OrdersGet200Response response =
        new org.openapitools.client.model.OrdersGet200Response()
            .data(p.getContent())
            .meta(
                new org.openapitools.client.model.PaginatedMeta()
                    .total(Math.toIntExact(p.getTotalElements()))
                    .page(p.getNumber() + 1)
                    .perPage(p.getSize())
                    .lastPage(p.getTotalPages()));
    return JSON.getGson().toJson(response);
  }

  @PostMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String createOrder(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @RequestBody String rawBody) {
    return JSON.getGson()
        .toJson(
            orderService.createOrder(
                parseOrderInput(rawBody),
                authSessionResolver.resolveUserOrNull(authorizationHeader)));
  }

  @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getOrder(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @PathVariable Long id) {
    return JSON.getGson()
        .toJson(
            orderService.getOrderForActor(
                id, authSessionResolver.resolveUserOrNull(authorizationHeader)));
  }

  @PutMapping(value = "/orders/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateStatus(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long id,
      @RequestBody String rawBody) {
    authSessionResolver.requireAdmin(authorizationHeader);
    org.openapitools.client.model.OrderStatusUpdate input = parseOrderStatusUpdate(rawBody);
    return JSON.getGson()
        .toJson(orderService.updateStatus(id, OrderStatus.valueOf(input.getStatus().name())));
  }

  @PostMapping(value = "/orders/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
  public String cancelOrder(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @PathVariable Long id) {
    return JSON.getGson()
        .toJson(
            orderService.cancelOrder(
                id, authSessionResolver.resolveUserOrNull(authorizationHeader)));
  }

  @GetMapping(value = "/orders/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listPayments(@PathVariable Long id) {
    List<org.openapitools.client.model.Payment> payments = orderService.listPayments(id);
    return JSON.getGson().toJson(payments);
  }

  @PostMapping(value = "/orders/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String initiatePayment(@PathVariable Long id, @RequestBody String rawBody) {
    System.out.println("post return : ");
    System.out.println(JSON.getGson().toJson(orderService.processPayment(id, parsePaymentInput(rawBody))));
    System.out.println("----------------");
    return JSON.getGson().toJson(orderService.processPayment(id, parsePaymentInput(rawBody)));
  }

  private org.openapitools.client.model.OrderInput parseOrderInput(String rawBody) {
    try {
      return org.openapitools.client.model.OrderInput.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid order payload");
    }
  }

  private org.openapitools.client.model.PaymentInput parsePaymentInput(String rawBody) {
    try {
      return org.openapitools.client.model.PaymentInput.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid payment payload");
    }
  }

  private org.openapitools.client.model.OrderStatusUpdate parseOrderStatusUpdate(String rawBody) {
    try {
      return org.openapitools.client.model.OrderStatusUpdate.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid order status payload");
    }
  }

  private OrderStatus parseOrderStatus(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }

    try {
      return OrderStatus.valueOf(rawValue.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid order status filter");
    }
  }
}
