package com.manuelfabri.expenses.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.service.TransactionService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping(Urls.TRANSACTION)
public class TransactionController {

  private TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping
  public ResponseEntity<Page<TransactionDto>> getAllTransactions(
      @RequestParam(required = false) TransactionTypeEnum type,
      @RequestParam(required = false) List<Long> categoryIds, @RequestParam(required = false) List<Long> subcategoryIds,
      @RequestParam(required = false) List<Long> accountIds, @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
      @PageableDefault(size = 20,
          page = 0) @SortDefault.SortDefaults({@SortDefault(sort = "eventDate", direction = Direction.DESC),
              @SortDefault(sort = "id", direction = Direction.DESC)}) Pageable pageable) {
    return new ResponseEntity<>(transactionService.getPagedTransactions(type, categoryIds, subcategoryIds, accountIds,
        minAmount, maxAmount, fromDate, toDate, pageable), HttpStatus.OK);
  }

  @GetMapping("/totals")
  public ResponseEntity<List<BalanceSummaryDto>> getFilteredTotals(
      @RequestParam(required = false) TransactionTypeEnum type,
      @RequestParam(required = false) List<Long> categoryIds, @RequestParam(required = false) List<Long> subcategoryIds,
      @RequestParam(required = false) List<Long> accountIds, @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
    return new ResponseEntity<>(transactionService.getFilteredTotals(type, categoryIds, subcategoryIds, accountIds,
        minAmount, maxAmount, fromDate, toDate), HttpStatus.OK);
  }

  @GetMapping("/pending")
  public ResponseEntity<List<TransactionDto>> getPendingTransactions() {
    return new ResponseEntity<>(transactionService.getPendingTransactions(), HttpStatus.OK);
  }

  @GetMapping("/{year}/{month}")
  public ResponseEntity<List<TransactionDto>> getByYearAndMonth(@PathVariable int year, @PathVariable int month) {
    return new ResponseEntity<>(transactionService.getMonthlyTransactions(year, month), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransactionDto> getById(@PathVariable Long id) {
    return new ResponseEntity<>(transactionService.getById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<TransactionDto> createTransaction(@RequestBody @Valid TransactionRequestDto transactionDto) {
    return new ResponseEntity<>(transactionService.createTransaction(transactionDto), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<TransactionDto> editTransaction(@PathVariable Long id,
      @RequestBody @Valid TransactionRequestDto transactionDto) {
    return new ResponseEntity<>(transactionService.updateTransaction(id, transactionDto), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<TransactionDto> deleteTransaction(@PathVariable Long id) {
    transactionService.deleteTransaction(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PatchMapping("/{id}/confirm")
  public ResponseEntity<TransactionDto> confirm(@PathVariable Long id) {
    return new ResponseEntity<>(transactionService.confirm(id), HttpStatus.OK);
  }

}
