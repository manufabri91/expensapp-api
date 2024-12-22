package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import com.manuelfabri.expenses.model.CurrencyEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceSummaryDto {
  @NotNull
  private CurrencyEnum currency;
  @NotNull
  private BigDecimal totalBalance;
  @NotNull
  private BigDecimal incomes;
  @NotNull
  private BigDecimal expenses;
}