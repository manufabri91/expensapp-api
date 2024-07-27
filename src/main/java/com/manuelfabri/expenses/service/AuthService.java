package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.UserLoginDto;
import com.manuelfabri.expenses.dto.UserLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRefreshTokenResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;

public interface AuthService {

  UserLoginResponseDto login(UserLoginDto userLoginData);

  UserLoginResponseDto register(UserRegisterDto userRegisterData);

  UserRefreshTokenResponseDto refresh(String refreshToken);

}
