package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequestDto {
  @NotBlank(message = "MISSING_NAME")
  private String name;
  @NotNull(message = "MISSING_ICON_NAME")
  private String iconName;
  @NotNull(message = "MISSING_COLOR")
  private String color;
}
