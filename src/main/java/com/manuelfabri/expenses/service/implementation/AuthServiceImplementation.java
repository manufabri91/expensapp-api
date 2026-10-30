package com.manuelfabri.expenses.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import com.manuelfabri.expenses.constants.Roles;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRefreshTokenResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.DependencyException;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.RoleRepository;
import com.manuelfabri.expenses.service.AuthService;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.UserService;

@Service
public class AuthServiceImplementation implements AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthServiceImplementation.class);

  private RoleRepository roleRepository;
  private UserService userService;
  private FirebaseService firebaseService;

  public AuthServiceImplementation(RoleRepository roleRepository, UserService userService,
      FirebaseService firebaseService) {
    this.roleRepository = roleRepository;
    this.userService = userService;
    this.firebaseService = firebaseService;
  }

  @Override
  public UserLoginResponseDto login(UserLoginDto userLoginData) {
    String email = userLoginData.getEmail();
    String password = userLoginData.getPassword();

    FirebaseLoginResponseDto authResponse = firebaseService.login(email, password);
    User user;
    try {
      user = userService.getUserById(authResponse.getLocalId());
    } catch (ResourceNotFoundException exception) {
      log.warn("Authentication stage=local-user-lookup requestId={} uid={} status=404",
          MDC.get("requestId"), authResponse.getLocalId());
      throw exception;
    }

    return authenticationResponse(authResponse, user);
  }

  private UserLoginResponseDto authenticationResponse(FirebaseLoginResponseDto authResponse, User user) {
    var dto = UserLoginResponseDto.fromFirebaseLoginResponse(authResponse);
    dto.setFirstName(user.getFirstName());
    dto.setLastName(user.getLastName());
    dto.setUsername(user.getUsername());

    return dto;

  }

  @Override
  public UserLoginResponseDto register(UserRegisterDto userRegisterData) {
    Role role =
        roleRepository.findByName(Roles.BASIC_USER).orElseThrow(() -> new DependencyException("RolesRepository"));
    var fbUser = firebaseService.createUserFromRequest(userRegisterData, role);

    User savedUser;
    try {
      savedUser = userService.createUserFromRequest(userRegisterData, fbUser.getUid(), role);
    } catch (RuntimeException dbException) {
      // Compensating action: the Firebase user was already created, but the local DB write
      // failed, so roll back the Firebase user to avoid stranding an account that can never
      // log in (no local User row) nor be re-registered (email already taken in Firebase).
      try {
        firebaseService.deleteUser(fbUser.getUid());
      } catch (RuntimeException cleanupException) {
        // Best effort cleanup only: surface the original DB failure below even if the
        // compensating Firebase deletion also fails, rather than masking the root cause.
        log.error("Failed to roll back Firebase user {} after registration DB failure; account is orphaned",
            fbUser.getUid());
      }
      throw dbException;
    }

    UserLoginResponseDto user;
    try {
      var authResponse = firebaseService.login(userRegisterData.getEmail(), userRegisterData.getPassword());
      if (authResponse == null || !StringUtils.hasText(authResponse.getIdToken())
          || !StringUtils.hasText(authResponse.getRefreshToken())) {
        throw new DependencyException("Firebase token response");
      }
      if (!fbUser.getUid().equals(authResponse.getLocalId())
          || !savedUser.getId().equals(authResponse.getLocalId())) {
        log.error("Registration stage=identity-mismatch requestId={} createdUid={} authenticatedUid={}",
            MDC.get("requestId"), fbUser.getUid(), authResponse.getLocalId());
        throw new DependencyException("Firebase project identity");
      }
      user = authenticationResponse(authResponse, savedUser);
      user.setRegistrationStatus("AUTHENTICATED");
    } catch (RuntimeException loginException) {
      // The Firebase + DB account already exists at this point (see the try/catch above), so a
      // failure here must not be reported as a failed registration. Return a degraded response
      // with no token; the client can retry via /auth/login.
      log.warn("Registration stage=sign-in requestId={} uid={} outcome=SIGN_IN_REQUIRED cause={}",
          MDC.get("requestId"), fbUser.getUid(), loginException.getClass().getSimpleName());
      var degradedResponse = new UserLoginResponseDto();
      degradedResponse.setEmail(userRegisterData.getEmail());
      degradedResponse.setRegistrationStatus("SIGN_IN_REQUIRED");
      return degradedResponse;
    }

    try {
      firebaseService.sendVerificationEmail(user.getToken());
    } catch (RuntimeException verificationException) {
      // Non-critical: the account and the login token are already valid, so don't fail
      // registration just because the verification email couldn't be sent.
      log.warn("Registration stage=verification requestId={} uid={} cause={}",
          MDC.get("requestId"), fbUser.getUid(), verificationException.getClass().getSimpleName());
    }

    return user;
  }

  @Override
  public UserRefreshTokenResponseDto refresh(String refreshToken) {
    return UserRefreshTokenResponseDto.fromFirebaseRefreshResponse(firebaseService.refreshToken(refreshToken));
  }
}
