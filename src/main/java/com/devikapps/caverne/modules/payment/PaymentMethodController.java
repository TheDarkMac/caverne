package com.devikapps.caverne.modules.payment;

import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PaymentMethodController {

  private final PaymentMethodService paymentMethodService;

  @GetMapping(value = "/payment-methods", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listPaymentMethods() {
    return JSON.getGson().toJson(paymentMethodService.listAll());
  }
}
