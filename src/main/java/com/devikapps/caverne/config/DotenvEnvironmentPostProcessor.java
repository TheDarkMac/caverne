package com.devikapps.caverne.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    Dotenv dotenv =
        Dotenv.configure().directory(System.getProperty("user.dir")).ignoreIfMissing().load();

    if (dotenv.entries().isEmpty()) {
      return;
    }

    Map<String, Object> properties = new LinkedHashMap<>();
    dotenv.entries().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));

    SystemEnvironmentPropertySource propertySource =
        new SystemEnvironmentPropertySource("dotenv", properties);

    if (environment
        .getPropertySources()
        .contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
      environment
          .getPropertySources()
          .addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
    } else {
      environment.getPropertySources().addLast(propertySource);
    }
  }
}
