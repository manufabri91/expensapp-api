package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginDto {

  @NotBlank
  @Email
  private String email;
  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 32, message = "Password should contain between 8 to 32 characters")
  private String password;

}
