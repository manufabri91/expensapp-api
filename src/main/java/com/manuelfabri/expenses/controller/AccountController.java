package com.manuelfabri.expenses.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.AccountDto;
import com.manuelfabri.expenses.dto.AccountRequestDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.service.AccountService;
import com.manuelfabri.expenses.service.TransactionService;

@RestController
@RequestMapping(Urls.ACCOUNT)
public class AccountController {

  private AccountService accountService;
  private TransactionService transactionService;

  public AccountController(AccountService accountService, TransactionService transactionService) {
    this.accountService = accountService;
    this.transactionService = transactionService;
  }

  @GetMapping
  public ResponseEntity<List<AccountDto>> getAllAccounts(Principal principal) {
    return new ResponseEntity<>(accountService.getAllAccounts(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AccountDto> getById(@PathVariable Long id) {
    return new ResponseEntity<>(accountService.getById(id), HttpStatus.OK);
  }

  @GetMapping("/{id}/transactions")
  public ResponseEntity<List<TransactionDto>> getTransactionsByAccountId(@PathVariable Long id) {
    return new ResponseEntity<>(transactionService.getTransactionsByAccountId(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<AccountDto> createAccount(@RequestBody AccountRequestDto accountDto) {
    return new ResponseEntity<>(accountService.createAccount(accountDto), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id,
      @RequestBody AccountRequestDto accountDto) {
    return new ResponseEntity<>(accountService.updateAccount(id, accountDto), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<AccountDto> deleteAccount(@PathVariable Long id) {
    accountService.deleteAccount(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
