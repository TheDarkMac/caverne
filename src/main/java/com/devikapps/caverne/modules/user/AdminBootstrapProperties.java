package com.devikapps.caverne.modules.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "bootstrap.admin")
public class AdminBootstrapProperties {
  private boolean enabled;
  private String firstname = "Initial";
  private String lastname = "Admin";
  private String email;
  private String phone;
  private String password;
}
