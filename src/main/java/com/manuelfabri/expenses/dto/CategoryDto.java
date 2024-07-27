package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.manuelfabri.expenses.model.CurrencyEnum;
import lombok.Data;

@Data
public class CategoryDto {
  @NotNull
  private long id;
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private CurrencyEnum iconName;
  @NotNull
  private CurrencyEnum color;
}
