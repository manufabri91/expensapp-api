package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import com.manuelfabri.expenses.model.CurrencyEnum;
import lombok.Data;

@Data
public class CreateAccountDto {
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private CurrencyEnum currency;
  private BigDecimal initialBalance;
}
