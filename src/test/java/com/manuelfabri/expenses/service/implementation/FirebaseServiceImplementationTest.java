package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.google.firebase.auth.*;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.*;
import com.manuelfabri.expenses.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class FirebaseServiceImplementationTest {
  private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final RestTemplateBuilder builder = mock(RestTemplateBuilder.class, RETURNS_SELF);
  private FirebaseServiceImplementation service;
  private final UserRegisterDto request = new UserRegisterDto();
  private final Role role = new Role(1L);

  @BeforeEach
  void setUp() {
    when(builder.build()).thenReturn(restTemplate);
    service = new FirebaseServiceImplementation("test-key", builder, firebaseAuth);
    request.setEmail("new@example.com");
    request.setPassword("password123");
    request.setFirstName("New");
    request.setLastName("User");
  }

  private FirebaseAuthException failure(AuthErrorCode code) {
    var exception = mock(FirebaseAuthException.class);
    when(exception.getAuthErrorCode()).thenReturn(code);
    return exception;
  }

  @Test
  void createAndClaimsSuccessUsesInjectedAuth() throws Exception {
    var identity = mock(UserRecord.class);
    when(identity.getUid()).thenReturn("new-uid");
    when(firebaseAuth.createUser(any())).thenReturn(identity);
    when(firebaseAuth.updateUser(any())).thenReturn(identity);
    assertThat(service.createUserFromRequest(request, role)).isSameAs(identity);
    verify(firebaseAuth, never()).deleteUser(any());
  }

  @Test
  void duplicateEmailHasDistinctErrorAndNeverDeletesExistingIdentity() throws Exception {
    var exception = failure(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    when(firebaseAuth.createUser(any())).thenThrow(exception);
    assertThatThrownBy(() -> service.createUserFromRequest(request, role)).isInstanceOf(DuplicateEmailException.class);
    verify(firebaseAuth, never()).deleteUser(any());
  }

  @Test
  void createDependencyFailureIsNotDuplicateAndDoesNotDeleteIdentity() throws Exception {
    var exception = failure(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.createUser(any())).thenThrow(exception);
    assertThatThrownBy(() -> service.createUserFromRequest(request, role)).isInstanceOf(DependencyException.class);
    verify(firebaseAuth, never()).deleteUser(any());
  }

  @Test
  void claimsFailureCompensatesCreatedIdentity() throws Exception {
    var identity = mock(UserRecord.class);
    when(identity.getUid()).thenReturn("new-uid");
    when(firebaseAuth.createUser(any())).thenReturn(identity);
    var exception = failure(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.updateUser(any())).thenThrow(exception);
    assertThatThrownBy(() -> service.createUserFromRequest(request, role)).isInstanceOf(DependencyException.class);
    verify(firebaseAuth).deleteUser("new-uid");
  }

  @Test
  void claimsCleanupFailurePreservesOriginalFailureCategory() throws Exception {
    var identity = mock(UserRecord.class);
    when(identity.getUid()).thenReturn("new-uid");
    when(firebaseAuth.createUser(any())).thenReturn(identity);
    var exception = failure(AuthErrorCode.USER_NOT_FOUND);
    var cleanupException = failure(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.updateUser(any())).thenThrow(exception);
    doThrow(cleanupException).when(firebaseAuth).deleteUser("new-uid");
    assertThatThrownBy(() -> service.createUserFromRequest(request, role)).isInstanceOf(DependencyException.class);
  }

  @Test
  void configuresBoundedConnectionAndReadTimeouts() {
    verify(builder).setConnectTimeout(java.time.Duration.ofSeconds(2));
    verify(builder).setReadTimeout(java.time.Duration.ofSeconds(3));
  }

  @Test
  void uncheckedClaimsAndCleanupFailuresStillReturnOriginalDependencyCategory() throws Exception {
    var identity = mock(UserRecord.class);
    when(identity.getUid()).thenReturn("new-uid");
    when(firebaseAuth.createUser(any())).thenReturn(identity);
    when(firebaseAuth.updateUser(any())).thenThrow(new IllegalArgumentException("private details"));
    doThrow(new IllegalStateException("private cleanup")).when(firebaseAuth).deleteUser("new-uid");
    assertThatThrownBy(() -> service.createUserFromRequest(request, role)).isInstanceOf(DependencyException.class)
        .hasMessageNotContaining("private");
    verify(firebaseAuth).deleteUser("new-uid");
  }

  @Test
  void loginParsesTokensFromRestResponse() {
    server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
        .andRespond(withSuccess("{\"idToken\":\"access\",\"refreshToken\":\"refresh\",\"localId\":\"new-uid\"}",
            MediaType.APPLICATION_JSON));
    assertThat(service.login("new@example.com", "password123").getIdToken()).isEqualTo("access");
    server.verify();
  }

  @Test
  void networkFailureProducesSanitizedDependencyError() {
    server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("secret-url")));
    assertThatThrownBy(() -> service.login("new@example.com", "password123"))
        .isInstanceOf(DependencyException.class).hasMessageNotContaining("secret-url");
  }

  @Test
  void parseTokenRejectsBlankAndInvalidTokensUsingInjectedAuth() throws Exception {
    assertThatThrownBy(() -> service.parseToken(" ")).isInstanceOf(IllegalArgumentException.class);
    var exception = failure(AuthErrorCode.INVALID_ID_TOKEN);
    when(firebaseAuth.verifyIdToken("invalid")).thenThrow(exception);
    assertThatThrownBy(() -> service.parseToken("invalid")).isInstanceOf(InvalidLoginException.class);
  }

  @Test
  void getAndDeleteUseInjectedAuthAndSanitizeDependencyFailure() throws Exception {
    var identity = mock(UserRecord.class);
    when(firebaseAuth.getUser("existing")).thenReturn(identity);
    assertThat(service.getUserById("existing")).isSameAs(identity);
    service.deleteUser("existing");
    verify(firebaseAuth).deleteUser("existing");
    var exception = failure(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.getUser("missing")).thenThrow(exception);
    doThrow(exception).when(firebaseAuth).deleteUser("missing");
    assertThatThrownBy(() -> service.getUserById("missing")).isInstanceOf(InvalidLoginException.class);
    assertThatThrownBy(() -> service.deleteUser("missing")).isInstanceOf(DependencyException.class);
  }

  @Test
  void refreshDependencyFailureIsSanitized() {
    server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("private-refresh")));
    assertThatThrownBy(() -> service.refreshToken("refresh-secret"))
        .isInstanceOf(DependencyException.class).hasMessageNotContaining("private-refresh");
  }
}
