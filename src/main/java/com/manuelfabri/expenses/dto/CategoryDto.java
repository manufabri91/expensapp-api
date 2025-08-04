package com.manuelfabri.expenses.dto;

import com.manuelfabri.expenses.model.TransactionTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryDto {
  @NotNull
  private long id;
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private String iconName;
  @NotNull
  private String color;
  @NotNull
  private TransactionTypeEnum type;
  @NotNull
  private Boolean readOnly;
}
