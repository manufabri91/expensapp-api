package com.manuelfabri.expenses.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.service.SummaryService;

@RestController
@RequestMapping(Urls.SUMMARY)
public class SummaryController {
  private SummaryService summaryService;

  public SummaryController(SummaryService summaryService) {
    this.summaryService = summaryService;
  }

  @GetMapping
  public ResponseEntity<List<BalanceSummaryDto>> getDefaultSummary(
      @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
      @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month) {
    return new ResponseEntity<>(this.summaryService.getMonthlySummaryWithTotalBalance(year, month), HttpStatus.OK);
  }
}
