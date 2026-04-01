package com.devikapps.caverne.modules.order;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String createOrder(@RequestBody String rawBody) {
        return JSON.getGson().toJson(orderService.createOrder(parseOrderInput(rawBody)));
    }

    @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getOrder(@PathVariable Long id) {
        return JSON.getGson().toJson(orderService.getOrder(id));
    }

    @PutMapping(value = "/orders/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateStatus(@PathVariable Long id, @RequestBody String rawBody) {
        org.openapitools.client.model.OrderStatusUpdate input = parseOrderStatusUpdate(rawBody);
        return JSON.getGson().toJson(orderService.updateStatus(
                id,
                OrderStatus.valueOf(input.getStatus().name())
        ));
    }

    @PostMapping(value = "/orders/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cancelOrder(@PathVariable Long id) {
        return JSON.getGson().toJson(orderService.updateStatus(id, OrderStatus.CANCELLED));
    }

    @GetMapping(value = "/orders/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    public String listPayments(@PathVariable Long id) {
        List<org.openapitools.client.model.Payment> payments = orderService.listPayments(id);
        return JSON.getGson().toJson(payments);
    }

    @PostMapping(value = "/orders/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String initiatePayment(
            @PathVariable Long id,
            @RequestBody String rawBody) {
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
}
