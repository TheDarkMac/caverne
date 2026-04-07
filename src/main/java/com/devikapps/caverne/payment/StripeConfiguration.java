package com.devikapps.caverne.payment;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfiguration {

  private final StripeProperties properties;

  public StripeConfiguration(StripeProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  public void init() {
    if (!properties.isEnabled()) {
      return;
    }
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw new IllegalStateException("stripe.api-key must be set when stripe.enabled=true");
    }
    if (!StringUtils.hasText(properties.getCheckoutSuccessUrl())) {
      throw new IllegalStateException(
          "stripe.checkout-success-url must be set when stripe.enabled=true");
    }
    if (!StringUtils.hasText(properties.getCheckoutCancelUrl())) {
      throw new IllegalStateException(
          "stripe.checkout-cancel-url must be set when stripe.enabled=true");
    }
    Stripe.apiKey = properties.getApiKey();
  }
}
