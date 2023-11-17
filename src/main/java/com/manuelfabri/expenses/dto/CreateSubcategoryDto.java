package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubcategoryDto {
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private Long parentCategoryId;
}
