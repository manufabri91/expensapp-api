package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubcategoryRequestDto {
  @NotBlank(message = "MISSING_NAME")
  private String name;
  @NotNull(message = "MISSING_PARENT_CATEGORY")
  private Long parentCategoryId;
}
