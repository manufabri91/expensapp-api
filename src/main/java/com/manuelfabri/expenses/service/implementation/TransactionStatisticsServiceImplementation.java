package com.manuelfabri.expenses.service.implementation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.checkerframework.checker.units.qual.s;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.CategoryTotalsDto;
import com.manuelfabri.expenses.dto.SubTotalsPerSubcategoryDto;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.service.TransactionStatisticsService;

@Service
public class TransactionStatisticsServiceImplementation implements TransactionStatisticsService {
  private TransactionRepository transactionRepository;

  public TransactionStatisticsServiceImplementation(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  private Map<CurrencyEnum, BigDecimal> parseTotalsByCurrency(List<Object[]> results) {
    return results.stream().collect(Collectors.toMap(result -> (CurrencyEnum) result[0], // Currency code
        result -> (BigDecimal) result[1] // Sum of amounts
    ));
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
    List<Object[]> results = this.transactionRepository.getBalancesByCurrency();
    return parseTotalsByCurrency(results);
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
    List<Object[]> totalsByCategory = this.transactionRepository.getTransactionsTotalExpensesByCategory(2024, 12);
    List<Object[]> totalsBySubcategory = this.transactionRepository.getTransactionsTotalExpensesBySubcategory(2024, 12);
    HashMap<Long, Map<CurrencyEnum, BigDecimal>> totalsPerCategory = new HashMap<>();
    HashMap<Long, String> categoryNames = new HashMap<>();
    HashMap<Long, List<SubTotalsPerSubcategoryDto>> subtotalsPerCategory = new HashMap<>();


    List<CategoryTotalsDto> totalsPerCategoryDto = new ArrayList<>();


    totalsBySubcategory.forEach(subcategoryTotal -> {
      var categoryExistsInHashMap = subtotalsPerCategory.get(subcategoryTotal[0]) != null;
      if (!categoryExistsInHashMap) {
        var categorySubtotals = new ArrayList<SubTotalsPerSubcategoryDto>();
        var subCategorySubtotal = new SubTotalsPerSubcategoryDto();
        var subtotal = new HashMap<CurrencyEnum, BigDecimal>();
        subtotal.put((CurrencyEnum) subcategoryTotal[3], (BigDecimal) subcategoryTotal[4]);
        subCategorySubtotal.setId((Long) subcategoryTotal[1]);
        subCategorySubtotal.setName((String) subcategoryTotal[2]);
        subCategorySubtotal.setSubtotals(subtotal);
        categorySubtotals.add(subCategorySubtotal);
        subtotalsPerCategory.put((Long) subcategoryTotal[0], categorySubtotals);
      } else {
        var a = subtotalsPerCategory.get(subcategoryTotal[0]);
        var subCategorySubtotal = a.stream()
            .filter(subTotal -> subTotal.getId().equals(subcategoryTotal[1]))
            .findFirst()
            .orElse(null);

        if (subCategorySubtotal == null) {
          subCategorySubtotal = new SubTotalsPerSubcategoryDto();
          subCategorySubtotal.setId((Long) subcategoryTotal[1]);
          subCategorySubtotal.setName((String) subcategoryTotal[2]);
          subCategorySubtotal.setSubtotals(new HashMap<>());
          a.add(subCategorySubtotal);
        }

        subCategorySubtotal.getSubtotals().put((CurrencyEnum) subcategoryTotal[3], (BigDecimal) subcategoryTotal[4]);
      }
    });

    totalsByCategory.forEach(categoryTotal -> {
      var categoryExistsInHashMap = totalsPerCategory.get(categoryTotal[0]) != null;
      if (!categoryExistsInHashMap) {
        totalsPerCategory.put((Long) categoryTotal[0], new HashMap<>());
        categoryNames.put((Long) categoryTotal[0], (String) categoryTotal[1]);
      }

      totalsPerCategory.get(categoryTotal[0]).put((CurrencyEnum) categoryTotal[2], (BigDecimal) categoryTotal[3]);
    });

    categoryNames.forEach((categoryId, categoryName) -> {
      var categoryTotalsDto = new CategoryTotalsDto();
      categoryTotalsDto.setId(categoryId);
      categoryTotalsDto.setName(categoryName);
      categoryTotalsDto.setTotals(totalsPerCategory.get(categoryId));
      categoryTotalsDto.setSubTotalsPerSubCategory(subtotalsPerCategory.get(categoryId));
      totalsPerCategoryDto.add(categoryTotalsDto);
    });

    return totalsPerCategoryDto;
  }

  @Override
  public List<CategoryTotalsDto> getTotalsPerCategory(int year) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getTotalsPerCategory'");
  }

  @Override
  public List<CategoryTotalsDto> getTotalsPerCategory(int year, int month) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getTotalsPerCategory'");
  }
}
