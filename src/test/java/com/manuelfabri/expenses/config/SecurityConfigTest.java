package com.manuelfabri.expenses.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real security filter chains (both {@link SecurityConfig} beans, plus
 * {@link com.manuelfabri.expenses.filter.FirebaseAuthorizationFilter}) end to end, unlike the
 * controller slice tests which disable security filters entirely. FirebaseApp/FirebaseAuth are
 * mocked out - the real beans need actual Google service-account credentials, which this test
 * doesn't need since it never calls Firebase.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FirebaseApp firebaseApp;

  @MockitoBean
  private FirebaseAuth firebaseAuth;

  @Test
  void actuatorHealth_isPubliclyAccessible_withoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void apiDocs_isPubliclyAccessible_withoutAuthentication() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void protectedEndpoint_withoutAuthorizationHeader_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/transaction")).andExpect(status().isUnauthorized());
  }
}
