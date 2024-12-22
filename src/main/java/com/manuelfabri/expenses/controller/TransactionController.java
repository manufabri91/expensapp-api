package com.manuelfabri.expenses.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(Urls.TRANSACTION)
public class TransactionController {

  private TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping
  public ResponseEntity<Page<TransactionDto>> getAllTransactions(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size, @RequestParam(defaultValue = "eventDate") String sortBy,
      @RequestParam(defaultValue = "false") boolean ascending) {
    Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    return new ResponseEntity<>(transactionService.getPagedTransactions(pageable), HttpStatus.OK);
  }

  @GetMapping("/{year}/{month}")
  public ResponseEntity<List<TransactionDto>> getByYearAndMonth(@PathVariable int year, @PathVariable int month) {
    return new ResponseEntity<>(transactionService.getMonthlyTransactions(year, month), HttpStatus.OK);
  }

  @GetMapping("/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getTotalsByCurrency() {
    return new ResponseEntity<>(transactionService.getBalancesByCurrency(), HttpStatus.OK);
  }

  @GetMapping("/expenses/{year}/{month}/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getExpensesTotalsByYearAndMonth(@PathVariable int year,
      @PathVariable int month) {
    return new ResponseEntity<>(transactionService.getExpensesTotalsByCurrency(year, month), HttpStatus.OK);
  }

  @GetMapping("/incomes/{year}/{month}/totals-by-currency")
  public ResponseEntity<Map<CurrencyEnum, BigDecimal>> getIncomesTotalsByYearAndMonth(@PathVariable int year,
      @PathVariable int month) {
    return new ResponseEntity<>(transactionService.getIncomesTotalsByCurrency(year, month), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransactionDto> getById(@PathVariable Long id, Principal principal) {
    return new ResponseEntity<>(transactionService.getById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<TransactionDto> createTransaction(@RequestBody @Valid TransactionRequestDto transactionDto) {
    return new ResponseEntity<>(transactionService.createTransaction(transactionDto), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<TransactionDto> deleteTransaction(@PathVariable Long id) {
    transactionService.deleteTransaction(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
