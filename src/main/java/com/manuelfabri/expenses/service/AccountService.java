package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.AccountDto;
import com.manuelfabri.expenses.dto.AccountRequestDto;


public interface AccountService {
  List<AccountDto> getAllAccounts();

  AccountDto createAccount(AccountRequestDto accountDto);

  AccountDto updateAccount(Long id, AccountRequestDto accountDto);

  void deleteAccount(Long id);

  AccountDto getById(Long id);

}
