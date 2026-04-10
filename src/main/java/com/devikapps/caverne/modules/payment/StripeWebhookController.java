package com.devikapps.caverne.modules.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhooks")
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StripeWebhookController {

  private final StripeWebhookService stripeWebhookService;

  @PostMapping("/stripe")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void handleStripeWebhook(
      @RequestHeader("Stripe-Signature") String signatureHeader, @RequestBody String payload) {
    stripeWebhookService.handleWebhook(payload, signatureHeader);
  }
}
