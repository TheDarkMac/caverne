package com.devikapps.caverne.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
  private boolean enabled;
  private String apiKey;
  private String apiVersion;
}
