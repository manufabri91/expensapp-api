package com.manuelfabri.expenses.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRefreshTokenResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@CrossOrigin
@RequestMapping(Urls.AUTH)
public class AuthController {

  private AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserLoginResponseDto> signUp(@Valid @RequestBody UserRegisterDto userRegisterData) {
    UserLoginResponseDto user = authService.register(userRegisterData);
    return ResponseEntity.ok(user);
  }

  @PostMapping("/login")
  public ResponseEntity<UserLoginResponseDto> signIn(@Valid @RequestBody UserLoginDto userLoginData) {
    UserLoginResponseDto user = authService.login(userLoginData);
    return ResponseEntity.ok(user);
  }

  @GetMapping("refresh")
  public ResponseEntity<UserRefreshTokenResponseDto> refreshToken(
      @RequestHeader("X-Refresh-Token") String xRefreshToken) {
    UserRefreshTokenResponseDto user = authService.refresh(xRefreshToken);
    return ResponseEntity.ok(user);
  }

}
