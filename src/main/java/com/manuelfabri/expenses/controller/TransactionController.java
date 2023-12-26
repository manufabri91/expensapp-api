package com.manuelfabri.expenses.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.CreateTransactionDto;
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
  public ResponseEntity<List<TransactionDto>> getAllTransactions() {

    return new ResponseEntity<>(transactionService.getAllTransactions(), HttpStatus.OK);
  }

  @GetMapping("/{year}/{month}")
  public ResponseEntity<List<TransactionDto>> getByYearAndMonth(@PathVariable("year") int year,
      @PathVariable("month") int month) {

    return new ResponseEntity<>(transactionService.getMonthlyTransactions(year, month), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransactionDto> getById(@PathVariable("id") Long id, Principal principal) {
    return new ResponseEntity<>(transactionService.getById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<TransactionDto> createTransaction(@RequestBody @Valid CreateTransactionDto transactionDto) {
    return new ResponseEntity<>(transactionService.createTransaction(transactionDto), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<TransactionDto> deleteTransaction(@PathVariable("id") Long id) {
    transactionService.deleteTransaction(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
