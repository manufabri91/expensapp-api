package com.manuelfabri.expenses.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
import com.manuelfabri.expenses.exception.*;
import com.manuelfabri.expenses.service.AuthService;

@WebMvcTest(controllers = AuthController.class, properties = "spring.jackson.use-jackson2-defaults=true")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
  @MockitoBean
  private AuthService auth;
  @Autowired
  private MockMvc mvc;
  private final String registration = """
      {"email":"new@example.com","password":"password123","userName":"new-user",
      "firstName":"New","lastName":"User","acceptedTerms":true}
      """;

  @Test
  void loginAcceptsCredentialsWithProductionJacksonDefaults() throws Exception {
    var response = new UserLoginResponseDto();
    response.setToken("access");
    response.setRefreshToken("refresh");
    when(auth.login(any())).thenReturn(response);

    mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
        {"email":"existing@example.com","password":"password123"}
        """))
        .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("access"))
        .andExpect(jsonPath("$.refreshToken").value("refresh"));

    ArgumentCaptor<UserLoginDto> credentials = ArgumentCaptor.forClass(UserLoginDto.class);
    verify(auth).login(credentials.capture());
    assertThat(credentials.getValue().getEmail()).isEqualTo("existing@example.com");
    assertThat(credentials.getValue().getPassword()).isEqualTo("password123");
  }

  @Test
  void invalidLoginReturnsValidationErrorsWithoutCallingAuthentication() throws Exception {
    mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors").isArray());
    verifyNoInteractions(auth);
  }

  @Test
  void registrationSerializesAdditiveDiscriminantAndExistingTokenFields() throws Exception {
    var response = new UserLoginResponseDto();
    response.setRegistrationStatus("AUTHENTICATED");
    response.setToken("access");
    response.setRefreshToken("refresh");
    when(auth.register(any())).thenReturn(response);
    mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
        .andExpect(status().isOk()).andExpect(jsonPath("$.registrationStatus").value("AUTHENTICATED"))
        .andExpect(jsonPath("$.token").value("access")).andExpect(jsonPath("$.refreshToken").value("refresh"));
    verify(auth, never()).login(any());
  }

  @Test
  void registrationRecoveryRemainsSuccessfulWithoutTokens() throws Exception {
    var response = new UserLoginResponseDto();
    response.setRegistrationStatus("SIGN_IN_REQUIRED");
    response.setEmail("new@example.com");
    when(auth.register(any())).thenReturn(response);
    mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
        .andExpect(status().isOk()).andExpect(jsonPath("$.registrationStatus").value("SIGN_IN_REQUIRED"));
  }

  @Test
  void duplicateAndDependencyErrorsRemainDistinct() throws Exception {
    when(auth.register(any())).thenThrow(new DuplicateEmailException());
    mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_EXISTS"));
    doThrow(new DependencyException("Firebase", "secret")).when(auth).register(any());
    mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
        .andExpect(status().isFailedDependency()).andExpect(jsonPath("$.code").value("DEPENDENCY_FAILURE"))
        .andExpect(jsonPath("$.message").value("Failed to execute dependency"));
  }

  @Test
  void invalidRegistrationRemains422AndDoesNotCreateAccount() throws Exception {
    mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors").isArray());
    verifyNoInteractions(auth);
  }
}
