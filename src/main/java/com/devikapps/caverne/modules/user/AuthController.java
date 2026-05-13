package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenProperties refreshTokenProperties;

  @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String register(@RequestBody String rawBody) {
    return JSON.getGson().toJson(authService.register(parseRegisterRequest(rawBody)));
  }

  @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
  public String login(@RequestBody String rawBody, HttpServletResponse response) {
    AuthService.AuthIssued issued = authService.login(parseLoginRequest(rawBody));
    setAuthCookies(response, issued);
    return JSON.getGson().toJson(issued.body());
  }

  @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
  public String refresh(HttpServletRequest request, HttpServletResponse response) {
    String rawRefresh = readCookie(request, refreshTokenProperties.getCookie().getName());
    if (rawRefresh == null) {
      throw new ResponseStatusException(UNAUTHORIZED, "Refresh token missing");
    }
    AuthService.AuthIssued issued = authService.refresh(rawRefresh);
    setAuthCookies(response, issued);
    return JSON.getGson().toJson(issued.body());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String rawRefresh = readCookie(request, refreshTokenProperties.getCookie().getName());
    authService.logout(rawRefresh);
    clearAuthCookies(response);
  }

  private void setAuthCookies(HttpServletResponse response, AuthService.AuthIssued issued) {
    long refreshMaxAge =
        Duration.between(LocalDateTime.now(), issued.refreshExpiresAt()).toSeconds();
    if (refreshMaxAge < 0) {
      refreshMaxAge = 0;
    }
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(
                refreshTokenProperties.getCookie().getName(),
                issued.rawRefreshToken(),
                refreshMaxAge,
                true)
            .toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(
                refreshTokenProperties.getCsrf().getCookieName(),
                issued.csrfToken(),
                refreshMaxAge,
                false)
            .toString());
  }

  private void clearAuthCookies(HttpServletResponse response) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(refreshTokenProperties.getCookie().getName(), "", 0, true).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(refreshTokenProperties.getCsrf().getCookieName(), "", 0, false).toString());
  }

  private ResponseCookie buildCookie(
      String name, String value, long maxAgeSeconds, boolean httpOnly) {
    RefreshTokenProperties.Cookie cfg = refreshTokenProperties.getCookie();
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(cfg.isSecure())
            .sameSite(cfg.getSameSite())
            .path(cfg.getPath())
            .maxAge(maxAgeSeconds);
    if (cfg.getDomain() != null && !cfg.getDomain().isBlank()) {
      builder.domain(cfg.getDomain());
    }
    return builder.build();
  }

  private String readCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private org.openapitools.client.model.RegisterRequest parseRegisterRequest(String rawBody) {
    try {
      return org.openapitools.client.model.RegisterRequest.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid registration payload");
    }
  }

  private org.openapitools.client.model.LoginRequest parseLoginRequest(String rawBody) {
    try {
      return org.openapitools.client.model.LoginRequest.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid login payload");
    }
  }
}
