package com.devikapps.caverne.modules.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTraceFilterTest {

  private final RequestTraceFilter filter = new RequestTraceFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void generatesUuidAndPutsItInMdcAndResponseHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    String[] observedTraceId = new String[1];
    doAnswer(
            inv -> {
              observedTraceId[0] = MDC.get(RequestTraceFilter.MDC_TRACE_ID);
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    filter.doFilter(request, response, chain);

    String header = response.getHeader(RequestTraceFilter.TRACE_ID_HEADER);
    assertThat(header).isNotBlank();
    assertThat(observedTraceId[0]).isEqualTo(header);
    // Fallback-generated traceId is a UUID
    assertThat(header)
        .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  }

  @Test
  void preservesExistingMdcTraceIdWhenAlreadyPopulated() throws Exception {
    MDC.put(RequestTraceFilter.MDC_TRACE_ID, "abc123-micrometer");

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestTraceFilter.TRACE_ID_HEADER))
        .isEqualTo("abc123-micrometer");
    // Existing traceId is not removed from MDC by the filter
    assertThat(MDC.get(RequestTraceFilter.MDC_TRACE_ID)).isEqualTo("abc123-micrometer");
  }

  @Test
  void clearsMdcTraceIdWhenItWasGeneratedLocally() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(MDC.get(RequestTraceFilter.MDC_TRACE_ID)).isNull();
  }
}
