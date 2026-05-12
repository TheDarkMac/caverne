package com.devikapps.caverne.modules.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.local.jwt")
public class LocalJwtProperties {
  private String secret;
  private String issuer = "caverne";
  private long expiresInSeconds = 3600;
}
