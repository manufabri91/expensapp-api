package com.manuelfabri.expenses.service.implementation;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.CategoryTotalsDto;
import com.manuelfabri.expenses.dto.MonthlyBalanceSummaryDto;
import com.manuelfabri.expenses.dto.SubTotalsPerSubcategoryDto;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.service.TransactionStatisticsService;

@Service
public class TransactionStatisticsServiceImplementation implements TransactionStatisticsService {
  private TransactionRepository transactionRepository;
  private AccountRepository accountRepository;

  public TransactionStatisticsServiceImplementation(TransactionRepository transactionRepository,
      AccountRepository accountRepository) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
  }

  private Map<CurrencyEnum, BigDecimal> parseTotalsByCurrency(List<Object[]> results) {
    return results.stream().collect(Collectors.toMap(result -> (CurrencyEnum) result[0], // Currency code
        result -> (BigDecimal) result[1] // Sum of amounts
    ));
  }

  private List<CategoryTotalsDto> mapTotalsQueriesToDto(List<Object[]> totalsByCategory,
      List<Object[]> totalsBySubcategory) {
    HashMap<Long, Map<CurrencyEnum, BigDecimal>> totalsPerCategory = new HashMap<>();
    HashMap<Long, String> categoryNames = new HashMap<>();
    HashMap<Long, String> categoryColors = new HashMap<>();
    HashMap<Long, List<SubTotalsPerSubcategoryDto>> subtotalsPerCategory = new HashMap<>();


    List<CategoryTotalsDto> totalsPerCategoryDto = new ArrayList<>();


    totalsBySubcategory.forEach(subcategoryTotal -> {
      var categoryExistsInHashMap = subtotalsPerCategory.get(subcategoryTotal[0]) != null;
      if (!categoryExistsInHashMap) {
        var categorySubtotals = new ArrayList<SubTotalsPerSubcategoryDto>();
        var subCategorySubtotal = new SubTotalsPerSubcategoryDto();
        var subtotal = new HashMap<CurrencyEnum, BigDecimal>();
        subCategorySubtotal.setId((Long) subcategoryTotal[1]);
        subCategorySubtotal.setName((String) subcategoryTotal[2]);
        subtotal.put((CurrencyEnum) subcategoryTotal[3], (BigDecimal) subcategoryTotal[4]);
        subCategorySubtotal.setSubtotals(subtotal);
        categorySubtotals.add(subCategorySubtotal);
        subtotalsPerCategory.put((Long) subcategoryTotal[0], categorySubtotals);
      } else {
        var mappedSubTotals = subtotalsPerCategory.get(subcategoryTotal[0]);
        var subCategorySubtotal = mappedSubTotals.stream()
            .filter(subTotal -> subTotal.getId().equals(subcategoryTotal[1])).findFirst().orElse(null);

        if (subCategorySubtotal == null) {
          subCategorySubtotal = new SubTotalsPerSubcategoryDto();
          subCategorySubtotal.setId((Long) subcategoryTotal[1]);
          subCategorySubtotal.setName((String) subcategoryTotal[2]);
          subCategorySubtotal.setSubtotals(new HashMap<>());
          mappedSubTotals.add(subCategorySubtotal);
        }

        subCategorySubtotal.getSubtotals().put((CurrencyEnum) subcategoryTotal[3], (BigDecimal) subcategoryTotal[4]);
      }
    });

    totalsByCategory.forEach(categoryTotal -> {
      var categoryExistsInHashMap = totalsPerCategory.get(categoryTotal[0]) != null;
      if (!categoryExistsInHashMap) {
        totalsPerCategory.put((Long) categoryTotal[0], new HashMap<>());
        categoryNames.put((Long) categoryTotal[0], (String) categoryTotal[1]);
        categoryColors.put((Long) categoryTotal[0], (String) categoryTotal[2]);
      }

      totalsPerCategory.get(categoryTotal[0]).put((CurrencyEnum) categoryTotal[3], (BigDecimal) categoryTotal[4]);
    });

    categoryNames.forEach((categoryId, categoryName) -> {
      var categoryTotalsDto = new CategoryTotalsDto();
      categoryTotalsDto.setId(categoryId);
      categoryTotalsDto.setName(categoryName);
      categoryTotalsDto.setColor(categoryColors.get(categoryId));
      categoryTotalsDto.setTotals(totalsPerCategory.get(categoryId));
      categoryTotalsDto.setSubTotalsPerSubCategory(subtotalsPerCategory.get(categoryId));
      totalsPerCategoryDto.add(categoryTotalsDto);
    });

    return totalsPerCategoryDto;
  }


  @Override
  public Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency() {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalIncomes();
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency(int year) {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalIncomes(year);
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getIncomesTotalsByCurrency(int year, int month) {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalIncomes(year, month);
    return this.parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency() {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalExpenses();
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency(int year) {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalExpenses(year);
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getExpensesTotalsByCurrency(int year, int month) {
    List<Object[]> results = this.transactionRepository.getTransactionsTotalExpenses(year, month);
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getBalancesByCurrency() {
    return this.accountRepository.findActive().stream().collect(Collectors.groupingBy(Account::getCurrency,
        Collectors.reducing(BigDecimal.ZERO, Account::getAccountBalance, BigDecimal::add)));
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getBalancesByCurrency(int year) {
    List<Object[]> results = this.transactionRepository.getBalancesByCurrency(year);
    return parseTotalsByCurrency(results);
  }

  @Override
  public Map<CurrencyEnum, BigDecimal> getBalancesByCurrency(int year, int month) {
    List<Object[]> results = this.transactionRepository.getBalancesByCurrency(year, month);
    return parseTotalsByCurrency(results);
  }


  @Override
  public List<CategoryTotalsDto> getTotalsPerCategory() {
    List<Object[]> totalsByCategory = this.transactionRepository.getTransactionsTotalExpensesByCategory();
    List<Object[]> totalsBySubcategory = this.transactionRepository.getTransactionsTotalExpensesBySubcategory();
    return mapTotalsQueriesToDto(totalsByCategory, totalsBySubcategory);
  }

  @Override
  public List<CategoryTotalsDto> getTotalsPerCategory(int year) {
    List<Object[]> totalsByCategory = this.transactionRepository.getTransactionsTotalExpensesByCategory(year);
    List<Object[]> totalsBySubcategory = this.transactionRepository.getTransactionsTotalExpensesBySubcategory(year);
    return mapTotalsQueriesToDto(totalsByCategory, totalsBySubcategory);
  }

  @Override
  public List<CategoryTotalsDto> getTotalsPerCategory(int year, int month) {
    List<Object[]> totalsByCategory = this.transactionRepository.getTransactionsTotalExpensesByCategory(year, month);
    List<Object[]> totalsBySubcategory =
        this.transactionRepository.getTransactionsTotalExpensesBySubcategory(year, month);
    return mapTotalsQueriesToDto(totalsByCategory, totalsBySubcategory);
  }

  @Override
  public List<MonthlyBalanceSummaryDto> getMonthlyHistory(int months) {
    return getMonthlyHistory(months, false);
  }

  @Override
  public List<MonthlyBalanceSummaryDto> getMonthlyHistory(int months, boolean includePending) {
    OffsetDateTime fromDate = OffsetDateTime.now().minusMonths(months - 1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

    List<Object[]> incomeResults;
    List<Object[]> expenseResults;
    if (includePending) {
      OffsetDateTime toDate =
          OffsetDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
      incomeResults = this.transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate);
      expenseResults =
          this.transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate);
    } else {
      incomeResults = this.transactionRepository.getTransactionsTotalIncomesByMonth(fromDate);
      expenseResults = this.transactionRepository.getTransactionsTotalExpensesByMonth(fromDate);
    }

    return buildMonthlyBalances(incomeResults, expenseResults);
  }

  private List<MonthlyBalanceSummaryDto> buildMonthlyBalances(List<Object[]> incomeResults,
      List<Object[]> expenseResults) {
    Map<String, MonthlyBalanceSummaryDto> monthlyBalancesByKey = new HashMap<>();

    incomeResults.forEach(row -> {
      var dto = getOrCreateMonthlyBalance(monthlyBalancesByKey, row);
      dto.setIncomes((BigDecimal) row[3]);
    });

    expenseResults.forEach(row -> {
      var dto = getOrCreateMonthlyBalance(monthlyBalancesByKey, row);
      dto.setExpenses((BigDecimal) row[3]);
    });

    return new ArrayList<>(monthlyBalancesByKey.values());
  }

  private MonthlyBalanceSummaryDto getOrCreateMonthlyBalance(Map<String, MonthlyBalanceSummaryDto> monthlyBalancesByKey,
      Object[] row) {
    int year = ((Number) row[0]).intValue();
    int month = ((Number) row[1]).intValue();
    CurrencyEnum currency = (CurrencyEnum) row[2];
    String key = year + "-" + month + "-" + currency;
    return monthlyBalancesByKey.computeIfAbsent(key, k -> {
      var dto = new MonthlyBalanceSummaryDto();
      dto.setYear(year);
      dto.setMonth(month);
      dto.setCurrency(currency);
      dto.setIncomes(BigDecimal.ZERO);
      dto.setExpenses(BigDecimal.ZERO);
      return dto;
    });
  }
}
