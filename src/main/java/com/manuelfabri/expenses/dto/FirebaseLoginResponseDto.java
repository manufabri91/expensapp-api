package com.manuelfabri.expenses.dto;

import lombok.Data;

@Data
public class FirebaseLoginResponseDto {
  private String idToken;
  private String email;
  private String refreshToken;
  private String expiresIn;
  private String localId;
  private boolean registered;
}
