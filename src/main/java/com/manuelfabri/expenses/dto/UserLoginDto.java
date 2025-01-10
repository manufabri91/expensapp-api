package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginDto {

  @NotBlank(message = "MISSING_EMAIL")
  @Email(message = "INVALID_EMAIL")
  private String email;
  @NotBlank(message = "BLANK_PASSWORD")
  @Size(min = 8, max = 32, message = "INVALID_PASSWORD_LENGTH")
  private String password;

}
