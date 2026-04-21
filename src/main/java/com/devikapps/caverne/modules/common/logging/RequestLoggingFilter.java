package com.devikapps.caverne.modules.common.logging;

import com.devikapps.caverne.modules.user.UserAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Emits one access-log line per HTTP request with method, URI, status, duration, client IP,
 * user-agent, and the authenticated userId (if any). Health endpoints are skipped to avoid noise.
 *
 * <p>Level is mapped from the response status: 2xx/3xx → INFO, 4xx → WARN, 5xx → ERROR. The userId
 * is also pushed into the MDC for the duration of the request so that any log emitted by downstream
 * handlers is tagged with it.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

  public static final String MDC_USER_ID = "userId";
  private static final Logger log = LoggerFactory.getLogger("http.access");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && (path.startsWith("/actuator") || path.contains("/actuator/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - start) / 1_000_000L;
      String userId = resolveUserId();
      if (userId != null) {
        MDC.put(MDC_USER_ID, userId);
      }
      try {
        int status = response.getStatus();
        String line = "{} {} -> {} in {}ms ip={} ua=\"{}\" user={}";
        Object[] args = {
          request.getMethod(),
          request.getRequestURI(),
          status,
          durationMs,
          clientIp(request),
          nullSafe(request.getHeader("User-Agent")),
          userId == null ? "-" : userId
        };
        if (status >= 500) {
          log.error(line, args);
        } else if (status >= 400) {
          log.warn(line, args);
        } else {
          log.info(line, args);
        }
      } finally {
        MDC.remove(MDC_USER_ID);
      }
    }
  }

  private static String resolveUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserAccount account && account.getId() != null) {
      return account.getId().toString();
    }
    return null;
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }

  private static String nullSafe(String value) {
    return value == null ? "-" : value;
  }
}
