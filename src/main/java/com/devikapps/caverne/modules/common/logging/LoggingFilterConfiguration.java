package com.devikapps.caverne.modules.common.logging;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link RequestTraceFilter} and {@link RequestLoggingFilter}.
 *
 * <p>Order matters. Spring's {@code ServerHttpObservationFilter} (registered by Micrometer Tracing)
 * runs near the beginning of the chain and opens a span whose traceId is pushed into the MDC. Our
 * filters must run AFTER it so they observe the MDC populated by Micrometer. We use {@code
 * HIGHEST_PRECEDENCE + 20_000_000} — earlier than security, later than observation.
 */
@Configuration
public class LoggingFilterConfiguration {

  private static final int TRACE_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 20_000_000;

  @Bean
  public FilterRegistrationBean<RequestTraceFilter> requestTraceFilterRegistration() {
    FilterRegistrationBean<RequestTraceFilter> registration =
        new FilterRegistrationBean<>(new RequestTraceFilter());
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
    registration.setOrder(TRACE_FILTER_ORDER);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration() {
    FilterRegistrationBean<RequestLoggingFilter> registration =
        new FilterRegistrationBean<>(new RequestLoggingFilter());
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
    registration.setOrder(TRACE_FILTER_ORDER + 10);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
