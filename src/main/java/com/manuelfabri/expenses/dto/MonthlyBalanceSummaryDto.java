package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import com.manuelfabri.expenses.model.CurrencyEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonthlyBalanceSummaryDto {
  @NotNull
  private int year;
  @NotNull
  private int month;
  @NotNull
  private CurrencyEnum currency;
  @NotNull
  private BigDecimal incomes;
  @NotNull
  private BigDecimal expenses;
}
