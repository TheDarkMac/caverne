package com.devikapps.caverne.modules.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.local.refresh")
public class RefreshTokenProperties {
  private long expiresInSeconds = 2_592_000L;
  private Cookie cookie = new Cookie();
  private Csrf csrf = new Csrf();

  @Getter
  @Setter
  public static class Cookie {
    private String name = "refresh_token";
    private String path = "/api/v1/auth";
    private String domain;
    private String sameSite = "None";
    private boolean secure = true;
  }

  @Getter
  @Setter
  public static class Csrf {
    private String cookieName = "csrf_token";
    private String headerName = "X-CSRF-Token";
  }
}
