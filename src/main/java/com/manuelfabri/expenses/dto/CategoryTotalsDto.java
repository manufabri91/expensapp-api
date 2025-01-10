package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.manuelfabri.expenses.model.CurrencyEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryTotalsDto {
  @NotNull
  private Long id;
  @NotNull
  private String name;
  @NotNull
  private Map<CurrencyEnum, BigDecimal> totals;
  @NotNull
  private List<SubTotalsPerSubcategoryDto> subTotalsPerSubCategory;
}
