package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.firebase.auth.UserRecord;
import com.manuelfabri.expenses.dto.*;
import com.manuelfabri.expenses.exception.*;
import com.manuelfabri.expenses.model.*;
import com.manuelfabri.expenses.repository.RoleRepository;
import com.manuelfabri.expenses.service.*;

class AuthServiceImplementationTest {
  private final RoleRepository roles = mock(RoleRepository.class);
  private final UserService users = mock(UserService.class);
  private final FirebaseService firebase = mock(FirebaseService.class);
  private final AuthServiceImplementation service = new AuthServiceImplementation(roles, users, firebase);
  private final UserRegisterDto request = new UserRegisterDto();
  private final Role role = new Role(1L);
  private final FirebaseLoginResponseDto authentication = new FirebaseLoginResponseDto();
  private User savedUser;

  @BeforeEach
  void setUp() {
    request.setEmail("new@example.com");
    request.setPassword("password123");
    request.setFirstName("New");
    request.setLastName("User");
    request.setUserName("new-user");
    savedUser = new User("new-uid", request.getEmail(), request.getUserName(), "New", "User", List.of(role));
    UserRecord identity = mock(UserRecord.class);
    when(identity.getUid()).thenReturn("new-uid");
    when(roles.findByName(anyString())).thenReturn(Optional.of(role));
    when(firebase.createUserFromRequest(request, role)).thenReturn(identity);
    when(users.createUserFromRequest(request, "new-uid", role)).thenReturn(savedUser);
    authentication.setLocalId("new-uid");
    authentication.setEmail(request.getEmail());
    authentication.setIdToken("access");
    authentication.setRefreshToken("refresh");
    authentication.setExpiresIn("3600");
    when(firebase.login(request.getEmail(), request.getPassword())).thenReturn(authentication);
  }

  @Test
  void registerUsesCommittedSavedUserAndReturnsAuthenticatedPayloadWithoutLocalReread() {
    var response = service.register(request);
    assertThat(response.getRegistrationStatus()).isEqualTo("AUTHENTICATED");
    assertThat(response.getToken()).isEqualTo("access");
    assertThat(response.getRefreshToken()).isEqualTo("refresh");
    assertThat(response.getUserId()).isEqualTo("new-uid");
    assertThat(response.getUsername()).isEqualTo("new-user");
    assertThat(response.getFirstName()).isEqualTo("New");
    assertThat(response.getLastName()).isEqualTo("User");
    verify(users, never()).getUserById(any());
    verify(firebase).sendVerificationEmail("access");
  }

  @Test
  void signInFailureRetainsAccountAndReturnsExplicitRecoveryOutcome() {
    when(firebase.login(anyString(), anyString())).thenThrow(new DependencyException());
    var response = service.register(request);
    assertThat(response.getRegistrationStatus()).isEqualTo("SIGN_IN_REQUIRED");
    assertThat(response.getEmail()).isEqualTo(request.getEmail());
    assertThat(response.getToken()).isNull();
    verify(firebase, never()).deleteUser(any());
    verify(firebase, never()).sendVerificationEmail(any());
  }

  @Test
  void tokenlessSignInResponseRetainsCreatedAccountWithRecoveryOutcome() {
    authentication.setRefreshToken(null);
    assertThat(service.register(request).getRegistrationStatus()).isEqualTo("SIGN_IN_REQUIRED");
    verify(firebase, never()).deleteUser(any());
    verify(firebase, never()).sendVerificationEmail(any());
  }

  @Test
  void mismatchedFirebaseRestProjectIdentityDoesNotIssueAuthenticatedResponse() {
    authentication.setLocalId("other-project-uid");
    var response = service.register(request);
    assertThat(response.getRegistrationStatus()).isEqualTo("SIGN_IN_REQUIRED");
    assertThat(response.getToken()).isNull();
    verify(firebase, never()).deleteUser(any());
    verify(firebase, never()).sendVerificationEmail(any());
  }

  @Test
  void verificationFailureRetainsAuthenticatedAccount() {
    doThrow(new IllegalStateException()).when(firebase).sendVerificationEmail(any());
    assertThat(service.register(request).getRegistrationStatus()).isEqualTo("AUTHENTICATED");
    verify(firebase, never()).deleteUser(any());
  }

  @Test
  void persistenceCommitFailureDeletesOnlyNewIdentityAndPreservesOriginalErrorWhenCleanupFails() {
    var failure = new IllegalStateException("commit failure");
    when(users.createUserFromRequest(request, "new-uid", role)).thenThrow(failure);
    doThrow(new DependencyException()).when(firebase).deleteUser("new-uid");
    assertThatThrownBy(() -> service.register(request)).isSameAs(failure);
    verify(firebase).deleteUser("new-uid");
    verify(firebase, never()).login(any(), any());
  }

  @Test
  void persistenceCommitFailureDeletesNewIdentity() {
    var failure = new IllegalStateException("commit failure");
    when(users.createUserFromRequest(request, "new-uid", role)).thenThrow(failure);
    assertThatThrownBy(() -> service.register(request)).isSameAs(failure);
    verify(firebase).deleteUser("new-uid");
  }

  @Test
  void missingRoleDoesNotCreateIdentity() {
    when(roles.findByName(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.register(request)).isInstanceOf(DependencyException.class);
    verify(firebase, never()).createUserFromRequest(any(), any());
  }

  @Test
  void ordinaryLoginMapsSameProfileAndPropagatesMissingLocalUser() {
    when(users.getUserById("new-uid")).thenReturn(savedUser);
    var login = new UserLoginDto(request.getEmail(), request.getPassword());
    assertThat(service.login(login).getUsername()).isEqualTo("new-user");
    when(users.getUserById("new-uid")).thenThrow(new ResourceNotFoundException("User", "id", "new-uid"));
    assertThatThrownBy(() -> service.login(login)).isInstanceOf(ResourceNotFoundException.class);
    verify(firebase, never()).createUserFromRequest(any(), any());
  }
}
