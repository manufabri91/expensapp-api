package com.manuelfabri.expenses.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.dto.CategoryTotalsDto;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.service.SummaryService;
import com.manuelfabri.expenses.service.TransactionStatisticsService;

@RestController
@RequestMapping(Urls.SUMMARY)
public class SummaryController {
  private SummaryService summaryService;
  private TransactionStatisticsService transactionStatisticsService;

  public SummaryController(SummaryService summaryService, TransactionStatisticsService transactionStatisticsService) {
    this.summaryService = summaryService;
    this.transactionStatisticsService = transactionStatisticsService;
  }

  @GetMapping
  public ResponseEntity<List<BalanceSummaryDto>> getDefaultSummary(
      @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
      @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month) {
    return new ResponseEntity<>(this.summaryService.getMonthlySummaryWithTotalBalance(year, month), HttpStatus.OK);
  }

  @GetMapping("/{year}/{month}")
  public ResponseEntity<List<BalanceSummaryDto>> getSummary(@PathVariable int year, @PathVariable int month) {
    return new ResponseEntity<>(this.summaryService.getSummary(year, month), HttpStatus.OK);
  }

  @GetMapping("/{year}")
  public ResponseEntity<List<BalanceSummaryDto>> getSummary(@PathVariable int year) {
    return new ResponseEntity<>(this.summaryService.getSummary(year), HttpStatus.OK);
  }

  @GetMapping("/historic")
  public ResponseEntity<List<BalanceSummaryDto>> getSummary() {
    return new ResponseEntity<>(this.summaryService.getSummary(), HttpStatus.OK);
  }

  @GetMapping("/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getTotalsByCurrency() {
    return new ResponseEntity<>(transactionStatisticsService.getBalancesByCurrency(), HttpStatus.OK);
  }

  @GetMapping("/totals-by-category")
  public ResponseEntity<List<CategoryTotalsDto>> getTotalsByCategory() {
    return new ResponseEntity<>(transactionStatisticsService.getTotalsPerCategory(), HttpStatus.OK);
  }

  @GetMapping("/totals-by-category/{year}")
  public ResponseEntity<List<CategoryTotalsDto>> getTotalsByCategory(@PathVariable int year) {
    return new ResponseEntity<>(transactionStatisticsService.getTotalsPerCategory(year), HttpStatus.OK);
  }

  @GetMapping("/totals-by-category/{year}/{month}")
  public ResponseEntity<List<CategoryTotalsDto>> getTotalsByCategory(@PathVariable int year, @PathVariable int month) {
    return new ResponseEntity<>(transactionStatisticsService.getTotalsPerCategory(year, month), HttpStatus.OK);
  }

  @GetMapping("/expenses/{year}/{month}/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getExpensesTotalsByYearAndMonth(@PathVariable int year,
      @PathVariable int month) {
    return new ResponseEntity<>(transactionStatisticsService.getExpensesTotalsByCurrency(year, month), HttpStatus.OK);
  }

  @GetMapping("/incomes/{year}/{month}/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getIncomesTotalsByYearAndMonth(@PathVariable int year,
      @PathVariable int month) {
    return new ResponseEntity<>(transactionStatisticsService.getIncomesTotalsByCurrency(year, month), HttpStatus.OK);
  }

}
