package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.CategoryTotalsDto;
import com.manuelfabri.expenses.model.CurrencyEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TransactionStatisticsService {
  Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency();

  Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency(int year);

  Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency(int year, int month);

  Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency();

  Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency(int year);

  Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency(int year, int month);

  Map<CurrencyEnum, BigDecimal> getBalancesByCurrency();

  Map<CurrencyEnum, BigDecimal> getBalancesByCurrency(int year);

  Map<CurrencyEnum, BigDecimal> getBalancesByCurrency(int year, int month);

  List<CategoryTotalsDto> getTotalsPerCategory();

  List<CategoryTotalsDto> getTotalsPerCategory(int year);

  List<CategoryTotalsDto> getTotalsPerCategory(int year, int month);
}
