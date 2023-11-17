package com.manuelfabri.expenses.service.implementation;

import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.constants.Roles;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
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
    userService.createUserFromRequest(userRegisterData, fbUser.getUid(), role);
    var loginData = new UserLoginDto(userRegisterData.getEmail(), userRegisterData.getPassword());
    var user = this.login(loginData);
    firebaseService.sendVerificationEmail(user.getToken());
    return user;
  }
}
