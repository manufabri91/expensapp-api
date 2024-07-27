package com.manuelfabri.expenses.dto;

import lombok.Data;

@Data
public class UserRefreshTokenResponseDto {
  private String expiresIn;
  private String tokenType;
  private String refreshToken;
  private String idToken;
  private String userId;

  public static UserRefreshTokenResponseDto fromFirebaseRefreshResponse(
      FirebaseRefreshTokenResponseDto fbRefreshResponse) {
    UserRefreshTokenResponseDto dto = new UserRefreshTokenResponseDto();
    dto.setExpiresIn(fbRefreshResponse.getExpiresIn());
    dto.setTokenType(fbRefreshResponse.getTokenType());
    dto.setRefreshToken(fbRefreshResponse.getRefreshToken());
    dto.setIdToken(fbRefreshResponse.getIdToken());
    dto.setUserId(fbRefreshResponse.getUserId());
    return dto;
  }
}
