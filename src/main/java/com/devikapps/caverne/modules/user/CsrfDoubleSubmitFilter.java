package com.devikapps.caverne.modules.user;

import com.devikapps.caverne.modules.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Double-submit cookie protection for the refresh / logout endpoints. The browser-readable
 * {@code csrf_token} cookie value must equal the {@code X-CSRF-Token} request header. Cross-origin
 * attackers cannot read the cookie, so they cannot forge a matching header.
 */
@Component
public class CsrfDoubleSubmitFilter extends OncePerRequestFilter {

  private static final Set<String> PROTECTED_PATHS = Set.of("/auth/refresh", "/auth/logout");

  private final RefreshTokenProperties properties;
  private final ObjectMapper objectMapper;

  public CsrfDoubleSubmitFilter(
      RefreshTokenProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod()) || !matchesProtectedPath(request)) {
      chain.doFilter(request, response);
      return;
    }

    // /auth/logout: CSRF is only meaningful when there is a refresh token to revoke. Supabase
    // users (or any token-only callers) never set the refresh cookie, so requiring CSRF there
    // would lock them out for no security benefit.
    boolean hasRefreshCookie =
        readCookie(request, properties.getCookie().getName()) != null;
    if (request.getRequestURI() != null
        && request.getRequestURI().endsWith("/auth/logout")
        && !hasRefreshCookie) {
      chain.doFilter(request, response);
      return;
    }

    String headerValue = request.getHeader(properties.getCsrf().getHeaderName());
    String cookieValue = readCookie(request, properties.getCsrf().getCookieName());

    if (headerValue == null
        || cookieValue == null
        || !constantTimeEquals(headerValue, cookieValue)) {
      writeForbidden(response, "CSRF token missing or invalid");
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean matchesProtectedPath(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) {
      return false;
    }
    for (String protectedPath : PROTECTED_PATHS) {
      if (uri.endsWith(protectedPath)) {
        return true;
      }
    }
    return false;
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

  private boolean constantTimeEquals(String a, String b) {
    byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
    byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(aBytes, bBytes);
  }

  private void writeForbidden(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        new ApiError(HttpStatus.FORBIDDEN.value(), message, MDC.get("traceId"), Instant.now()));
  }
}
