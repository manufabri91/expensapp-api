package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import com.manuelfabri.expenses.model.CurrencyEnum;
import lombok.Data;

@Data
public class AccountRequestDto {
  @NotBlank(message = "MISSING_NAME")
  private String name;
  @NotNull(message = "MISSING_CURRENCY")
  private CurrencyEnum currency;
  private BigDecimal initialBalance;
}
