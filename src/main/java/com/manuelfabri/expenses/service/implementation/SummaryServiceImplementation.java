package com.manuelfabri.expenses.service.implementation;

import com.manuelfabri.expenses.service.CategoryService;
import com.manuelfabri.expenses.service.SummaryService;
import com.manuelfabri.expenses.service.TransactionStatisticsService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.model.CurrencyEnum;

@Service
public class SummaryServiceImplementation implements SummaryService {
  private final TransactionStatisticsService transactionStatisticsService;
  private final CategoryService categoryService;

  public SummaryServiceImplementation(
      TransactionStatisticsService transactionStatisticsService, CategoryService categoryService) {
    this.transactionStatisticsService = transactionStatisticsService;
    this.categoryService = categoryService;
  }

  private List<BalanceSummaryDto> getSummaries(Map<CurrencyEnum, BigDecimal> balances,
      Map<CurrencyEnum, BigDecimal> incomes, Map<CurrencyEnum, BigDecimal> expenses) {
    var summariesHashMap = new HashMap<CurrencyEnum, BalanceSummaryDto>();
    balances.entrySet().stream().forEach(entry -> {
      var summary = new BalanceSummaryDto();
      var totalIncomes = incomes.get(entry.getKey());
      var totalExpenses = expenses.get(entry.getKey());
      summary.setCurrency(entry.getKey());
      summary.setTotalBalance(entry.getValue());
      summary.setIncomes(totalIncomes == null ? BigDecimal.ZERO : totalIncomes);
      summary.setExpenses(totalExpenses == null ? BigDecimal.ZERO : totalExpenses);
      summariesHashMap.put(entry.getKey(), summary);
    });

    return summariesHashMap.values().stream().toList();
  }

  @Override
  public List<BalanceSummaryDto> getSummary() {
    var balances = transactionStatisticsService.getBalancesByCurrency();
    var incomes = transactionStatisticsService.getIncomesTotalsByCurrency();
    var expenses = transactionStatisticsService.getExpensesTotalsByCurrency();
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getSummary(int year) {
    var balances = transactionStatisticsService.getBalancesByCurrency(year);
    var incomes = transactionStatisticsService.getIncomesTotalsByCurrency(year);
    var expenses = transactionStatisticsService.getExpensesTotalsByCurrency(year);
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getSummary(int year, int month) {
    var balances = transactionStatisticsService.getBalancesByCurrency(year, month);
    var incomes = transactionStatisticsService.getIncomesTotalsByCurrency(year, month);
    var expenses = transactionStatisticsService.getExpensesTotalsByCurrency(year, month);
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getMonthlySummaryWithTotalBalance(int year, int month) {
    var balances = transactionStatisticsService.getBalancesByCurrency();
    var incomes = transactionStatisticsService.getIncomesTotalsByCurrency(year, month);
    var expenses = transactionStatisticsService.getExpensesTotalsByCurrency(year, month);
    return getSummaries(balances, incomes, expenses);
  }

}
