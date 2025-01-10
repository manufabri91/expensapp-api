package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.util.Map;
import com.manuelfabri.expenses.model.CurrencyEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubTotalsPerSubcategoryDto {
  @NotNull
  private Long id;
  @NotNull
  private String name;
  @NotNull
  private Map<CurrencyEnum, BigDecimal> subtotals;
}
