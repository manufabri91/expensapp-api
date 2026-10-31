package com.manuelfabri.expenses.filter;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {
  @Test
  void validRequestIdReachesDiagnosticsAndResponseAndIsCleared() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "registration-123");
    var response = new MockHttpServletResponse();
    new RequestIdFilter().doFilter(request, response, (incoming, outgoing) ->
        assertThat(MDC.get("requestId")).isEqualTo("registration-123"));
    assertThat(response.getHeader("X-Request-ID")).isEqualTo("registration-123");
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void unsafeRequestIdIsReplacedAndContextClearedEvenOnFailure() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "unsafe\nvalue");
    var response = new MockHttpServletResponse();
    assertThatThrownBy(() -> new RequestIdFilter().doFilter(request, response, (incoming, outgoing) -> {
      throw new IllegalStateException();
    })).isInstanceOf(IllegalStateException.class);
    assertThat(response.getHeader("X-Request-ID")).matches("[a-f0-9-]{36}");
    assertThat(MDC.get("requestId")).isNull();
  }
}
