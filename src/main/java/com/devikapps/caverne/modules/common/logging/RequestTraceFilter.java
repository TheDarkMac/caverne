package com.devikapps.caverne.modules.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Echoes the current request's traceId to the client via the {@code X-Trace-Id} response header so
 * the caller can reference it when reporting an error.
 *
 * <p>If Micrometer Tracing is active (auto-configured by {@code micrometer-tracing-bridge-brave}),
 * the traceId is already in the MDC by the time this filter runs — we use it as-is. Otherwise we
 * generate a UUID fallback and push it to MDC ourselves.
 *
 * <p>This filter is registered to run AFTER Spring's {@code ServerHttpObservationFilter} so the
 * Micrometer span scope (and thus the MDC traceId) is open when we read it.
 */
public class RequestTraceFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String MDC_TRACE_ID = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String existing = MDC.get(MDC_TRACE_ID);
    boolean fallback = (existing == null || existing.isBlank());
    String traceId = fallback ? UUID.randomUUID().toString() : existing;
    if (fallback) {
      MDC.put(MDC_TRACE_ID, traceId);
    }
    response.setHeader(TRACE_ID_HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (fallback) {
        MDC.remove(MDC_TRACE_ID);
      }
    }
  }
}
