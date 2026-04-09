package com.devikapps.caverne.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
  private List<String> allowedOrigins = new ArrayList<>();
  private List<String> allowedMethods = new ArrayList<>();
  private List<String> allowedHeaders = new ArrayList<>();
  private List<String> exposedHeaders = new ArrayList<>();
  private boolean allowCredentials = true;
  private long maxAge = 3600;
}
