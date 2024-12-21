package com.manuelfabri.expenses.service.implementation;

import com.manuelfabri.expenses.service.CategoryService;
import com.manuelfabri.expenses.service.SubcategoryService;
import com.manuelfabri.expenses.service.SummaryService;
import com.manuelfabri.expenses.service.TransactionService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.units.qual.t;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.model.CurrencyEnum;

@Service
public class SummaryServiceImplementation implements SummaryService {
  private final TransactionService transactionService;

  public SummaryServiceImplementation(TransactionService transactionService, CategoryService categoryService,
      SubcategoryService subcategoryService) {
    this.transactionService = transactionService;
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
    var balances = transactionService.getBalancesByCurrency();
    var incomes = transactionService.getIncomesTotalsByCurrency();
    var expenses = transactionService.getExpensesTotalsByCurrency();
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getSummary(int year) {
    var balances = transactionService.getBalancesByCurrency(year);
    var incomes = transactionService.getIncomesTotalsByCurrency(year);
    var expenses = transactionService.getExpensesTotalsByCurrency(year);
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getSummary(int year, int month) {
    var balances = transactionService.getBalancesByCurrency(year, month);
    var incomes = transactionService.getIncomesTotalsByCurrency(year, month);
    var expenses = transactionService.getExpensesTotalsByCurrency(year, month);
    return getSummaries(balances, incomes, expenses);
  }

  @Override
  public List<BalanceSummaryDto> getMonthlySummaryWithTotalBalance(int year, int month) {
    var balances = transactionService.getBalancesByCurrency();
    var incomes = transactionService.getIncomesTotalsByCurrency(year, month);
    var expenses = transactionService.getExpensesTotalsByCurrency(year, month);
    return getSummaries(balances, incomes, expenses);
  }
}
