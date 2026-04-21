package com.devikapps.caverne.modules.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

  private final RequestLoggingFilter filter = new RequestLoggingFilter();
  private ListAppender<ILoggingEvent> appender;
  private Logger accessLogger;

  @BeforeEach
  void setUp() {
    accessLogger = (Logger) LoggerFactory.getLogger("http.access");
    accessLogger.setLevel(Level.TRACE);
    appender = new ListAppender<>();
    appender.start();
    accessLogger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    accessLogger.detachAppender(appender);
  }

  @Test
  void logsSuccessRequestAtInfoLevel() throws Exception {
    MockHttpServletRequest request = newRequest("GET", "/products");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(200);

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(appender.list).hasSize(1);
    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.INFO);
    assertThat(event.getFormattedMessage()).contains("GET").contains("/products").contains("200");
  }

  @Test
  void logsClientErrorAtWarnLevel() throws Exception {
    MockHttpServletRequest request = newRequest("POST", "/orders");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(422);

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.WARN);
  }

  @Test
  void logsServerErrorAtErrorLevel() throws Exception {
    MockHttpServletRequest request = newRequest("GET", "/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(500);

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
  }

  @Test
  void skipsActuatorEndpoints() throws Exception {
    MockHttpServletRequest request = newRequest("GET", "/actuator/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(200);

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(appender.list).isEmpty();
  }

  @Test
  void includesIpAndUserAgentInMessage() throws Exception {
    MockHttpServletRequest request = newRequest("GET", "/x");
    request.addHeader("User-Agent", "JUnit/5");
    request.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(200);

    filter.doFilter(request, response, mock(FilterChain.class));

    String message = appender.list.getFirst().getFormattedMessage();
    assertThat(message).contains("ip=10.0.0.1").contains("ua=\"JUnit/5\"");
  }

  @Test
  void prefersForwardedIpWhenPresent() throws Exception {
    MockHttpServletRequest request = newRequest("GET", "/x");
    request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
    request.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(200);

    filter.doFilter(request, response, mock(FilterChain.class));

    assertThat(appender.list.getFirst().getFormattedMessage()).contains("ip=203.0.113.42");
  }

  private static MockHttpServletRequest newRequest(String method, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRequestURI(uri);
    return request;
  }
}
