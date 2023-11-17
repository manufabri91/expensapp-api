package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDto {

  @NotBlank
  @Email
  private String email;
  @NotBlank
  @Size(min = 8, max = 32, message = "Username between 8 to 32 characters") // TODO: Create custom validator to handle
                                                                            // whitespaces in this field
  private String userName;
  @NotBlank
  @Size(min = 8, max = 32, message = "Password should contain between 8 to 32 characters")
  private String password;
  @NotBlank
  private String firstName;
  @NotBlank
  private String lastName;

}
