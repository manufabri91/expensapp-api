package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.AccountDto;
import com.manuelfabri.expenses.dto.CreateAccountDto;


public interface AccountService {
  List<AccountDto> getAllAccounts();

  AccountDto createAccount(CreateAccountDto accountDto);

  void deleteAccount(Long id);

  AccountDto getById(Long id);

}
