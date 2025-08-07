package com.manuelfabri.expenses.dto;

import com.manuelfabri.expenses.model.TransactionTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequestDto {
  @NotBlank(message = "MISSING_NAME")
  private String name;
  private String iconName;
  private String color;
  @NotNull(message = "MISSING_TYPE")
  private TransactionTypeEnum type;
}
