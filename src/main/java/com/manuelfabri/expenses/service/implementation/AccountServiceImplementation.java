package com.manuelfabri.expenses.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.AccountDto;
import com.manuelfabri.expenses.dto.AccountRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.service.AccountService;

@Service
public class AccountServiceImplementation implements AccountService {
  private AccountRepository accountRepository;
  private ModelMapper mapper;

  public AccountServiceImplementation(AccountRepository accountRepository, ModelMapper mapper) {

    this.accountRepository = accountRepository;
    this.mapper = mapper;
  }

  @Override
  public AccountDto createAccount(AccountRequestDto accountDto) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Account account = mapper.map(accountDto, Account.class);
    account.setOwner(user);
    Account newAccount = this.accountRepository.save(account);

    return mapper.map(newAccount, AccountDto.class);
  }

  @Override
  public List<AccountDto> getAllAccounts() {
    return this.accountRepository.findActive().stream().map(account -> mapper.map(account, AccountDto.class))
        .collect(Collectors.toList());
  }

  @Override
  public AccountDto getById(Long id) {
    return this.accountRepository.findActiveById(id).map((acct) -> mapper.map(acct, AccountDto.class))
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id.toString()));
  }

  @Override
  public AccountDto updateAccount(Long id, AccountRequestDto accountDto) {
    Account account = this.accountRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id.toString()));
    account.setName(accountDto.getName());
    account.setCurrency(accountDto.getCurrency());
    account.setInitialBalance(accountDto.getInitialBalance());

    Account updatedAccount = this.accountRepository.save(account);

    return mapper.map(updatedAccount, AccountDto.class);
  }

  @Override
  public void deleteAccount(Long id) {
    Account account = this.accountRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id.toString()));
    this.accountRepository.delete(account);
  }

}
