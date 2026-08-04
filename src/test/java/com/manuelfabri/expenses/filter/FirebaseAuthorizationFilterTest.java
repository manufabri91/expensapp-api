package com.manuelfabri.expenses.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.UserService;

class FirebaseAuthorizationFilterTest {

  private final UserService userService = mock(UserService.class);
  private final FirebaseService firebaseService = mock(FirebaseService.class);
  private final FirebaseAuthorizationFilter filter = new FirebaseAuthorizationFilter(userService, firebaseService);

  @Test
  void doFilterInternal_bypassesAuthorization_forApiDocsPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(firebaseService, never()).parseToken(any());
  }

  @Test
  void doFilterInternal_bypassesAuthorization_forSwaggerUiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(firebaseService, never()).parseToken(any());
  }

  @Test
  void doFilterInternal_unexpectedException_writesInternalServerErrorJson() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/transaction");
    request.addHeader("Authorization", "Bearer some-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(firebaseService.parseToken("Bearer some-token")).thenThrow(new RuntimeException("firebase is down"));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getContentAsString()).contains("An unexpected error occurred.");
    verify(filterChain, never()).doFilter(any(), any());
  }
}
