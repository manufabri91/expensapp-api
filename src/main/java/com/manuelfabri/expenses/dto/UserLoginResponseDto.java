package com.manuelfabri.expenses.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {
  private String userId;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String token;
  private String refreshToken;
  private String tokenExpiresIn;

  public static UserLoginResponseDto fromFirebaseLoginResponse(FirebaseLoginResponseDto fbLoginResponse) {
    UserLoginResponseDto dto = new UserLoginResponseDto();
    dto.setUserId(fbLoginResponse.getLocalId());
    dto.setToken(fbLoginResponse.getIdToken());
    dto.setRefreshToken(fbLoginResponse.getRefreshToken());
    dto.setEmail(fbLoginResponse.getEmail());
    dto.setTokenExpiresIn(fbLoginResponse.getExpiresIn());
    return dto;
  }
}
