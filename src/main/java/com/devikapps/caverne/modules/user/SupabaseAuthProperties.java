package com.devikapps.caverne.modules.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.providers.supabase")
public class SupabaseAuthProperties {
  private boolean enabled;
  private String jwtSecret;
  private String issuer;
  private String audience = "authenticated";
}
