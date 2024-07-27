package com.manuelfabri.expenses.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FirebaseRefreshTokenResponseDto {
  @JsonProperty("expires_in")
  private String expiresIn;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("id_token")
  private String idToken;

  @JsonProperty("user_id")
  private String userId;

  @JsonProperty("project_id")
  private String projectId;
}
