package com.devikapps.caverne.modules.payment;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

  private final Optional<StripeWebhookService> stripeWebhookService;

  @PostMapping("/stripe")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void handleStripeWebhook(
      @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader,
      @RequestBody String payload) {
    if (stripeWebhookService.isEmpty()) {
      return;
    }
    stripeWebhookService.get().handleWebhook(payload, signatureHeader);
  }
}
