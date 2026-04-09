package com.devikapps.caverne.modules.catalog;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "catalog.niches-import")
public class NichesImportProperties {
  private boolean enabled;
  private String path = "niches.md";
}
