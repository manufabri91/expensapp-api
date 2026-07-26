package com.manuelfabri.expenses.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.manuelfabri.expenses.constants.Roles;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRefreshTokenResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.DependencyException;
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
    User user = userService.getUserById(authResponse.getLocalId());

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

    try {
      userService.createUserFromRequest(userRegisterData, fbUser.getUid(), role);
    } catch (RuntimeException dbException) {
      // Compensating action: the Firebase user was already created, but the local DB write
      // failed, so roll back the Firebase user to avoid stranding an account that can never
      // log in (no local User row) nor be re-registered (email already taken in Firebase).
      try {
        FirebaseAuth.getInstance().deleteUser(fbUser.getUid());
      } catch (FirebaseAuthException cleanupException) {
        // Best effort cleanup only: surface the original DB failure below even if the
        // compensating Firebase deletion also fails, rather than masking the root cause.
        log.error("Failed to roll back Firebase user {} after registration DB failure; account is orphaned",
            fbUser.getUid(), cleanupException);
      }
      throw dbException;
    }

    var loginData = new UserLoginDto(userRegisterData.getEmail(), userRegisterData.getPassword());
    UserLoginResponseDto user;
    try {
      user = this.login(loginData);
    } catch (RuntimeException loginException) {
      // The Firebase + DB account already exists at this point (see the try/catch above), so a
      // failure here must not be reported as a failed registration. Return a degraded response
      // with no token; the client can retry via /auth/login.
      log.warn("Registration succeeded for {} but the immediate auto-login failed; "
          + "client must retry via /auth/login", userRegisterData.getEmail(), loginException);
      var degradedResponse = new UserLoginResponseDto();
      degradedResponse.setEmail(userRegisterData.getEmail());
      return degradedResponse;
    }

    try {
      firebaseService.sendVerificationEmail(user.getToken());
    } catch (RuntimeException verificationException) {
      // Non-critical: the account and the login token are already valid, so don't fail
      // registration just because the verification email couldn't be sent.
      log.warn("Registration succeeded for {} but sending the verification email failed",
          userRegisterData.getEmail(), verificationException);
    }

    return user;
  }

  @Override
  public UserRefreshTokenResponseDto refresh(String refreshToken) {
    return UserRefreshTokenResponseDto.fromFirebaseRefreshResponse(firebaseService.refreshToken(refreshToken));
  }
}
