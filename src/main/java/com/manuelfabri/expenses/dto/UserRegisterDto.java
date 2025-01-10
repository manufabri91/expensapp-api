package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDto {

  @NotBlank(message = "MISSING_EMAIL")
  @Email(message = "INVALID_EMAIL")
  private String email;
  @NotBlank(message = "MISSING_USERNAME")
  @Size(min = 8, max = 32, message = "INVALID_USERNAME") // TODO: Create custom validator to handle
                                                         // whitespaces in this field
  private String userName;
  @NotBlank(message = "MISSING_PASSWORD")
  @Size(min = 8, max = 32, message = "INVALID_PASSWORD_LENGTH")
  private String password;
  @NotBlank(message = "MISSING_FIRST_NAME")
  private String firstName;
  @NotBlank(message = "MISSING_LAST_NAME")
  private String lastName;

}
