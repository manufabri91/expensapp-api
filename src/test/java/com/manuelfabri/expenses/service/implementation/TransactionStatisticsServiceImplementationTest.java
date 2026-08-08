package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;

/**
 * Focused on {@code getBalancesByCurrency()} and its two overloads. The no-arg overload was rewritten to sum each
 * active account's {@code accountBalance} (the {@code @Formula}-computed running balance, which already includes
 * {@code initialBalance}) instead of delegating to a repository query over transactions alone - see the class under
 * test and {@code Account.accountBalance}. The year/month overloads are unchanged (still period net-change queries
 * delegated straight to {@link TransactionRepository}), so this file also regression-tests that they keep doing
 * that and never touch {@link AccountRepository}.
 */
@ExtendWith(MockitoExtension.class)
class TransactionStatisticsServiceImplementationTest {

  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private AccountRepository accountRepository;

  private TransactionStatisticsServiceImplementation service;
  private User owner;

  private void createService() {
    service = new TransactionStatisticsServiceImplementation(transactionRepository, accountRepository);
  }

  private Account account(Long id, CurrencyEnum currency, String accountBalance) {
    if (owner == null) {
      owner = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    }
    Account account = new Account(id, "Account " + id, currency, owner);
    account.setAccountBalance(new BigDecimal(accountBalance));
    return account;
  }

  // --- getBalancesByCurrency() (no-arg) ---------------------------------------------------------------------------

  @Test
  void getBalancesByCurrency_sumsActiveAccountsGroupedByCurrency() {
    createService();
    Account first = account(1L, CurrencyEnum.USD, "100.00");
    Account second = account(2L, CurrencyEnum.USD, "50.00");
    when(accountRepository.findActive()).thenReturn(List.of(first, second));

    Map<CurrencyEnum, BigDecimal> result = service.getBalancesByCurrency();

    assertThat(result).hasSize(1);
    assertThat(result.get(CurrencyEnum.USD)).isEqualByComparingTo("150.00");
  }

  @Test
  void getBalancesByCurrency_includesAnAccountWithOnlyInitialBalanceAndNoTransactions() {
    createService();
    Account zeroActivityAccount = account(1L, CurrencyEnum.USD, "250.00");
    when(accountRepository.findActive()).thenReturn(List.of(zeroActivityAccount));

    Map<CurrencyEnum, BigDecimal> result = service.getBalancesByCurrency();

    assertThat(result.get(CurrencyEnum.USD)).isEqualByComparingTo("250.00");
  }

  @Test
  void getBalancesByCurrency_producesSeparateEntriesForDifferentCurrencies() {
    createService();
    Account usdAccount = account(1L, CurrencyEnum.USD, "100.00");
    Account eurAccount = account(2L, CurrencyEnum.EUR, "75.00");
    Account arsAccount = account(3L, CurrencyEnum.ARS, "500.00");
    when(accountRepository.findActive()).thenReturn(List.of(usdAccount, eurAccount, arsAccount));

    Map<CurrencyEnum, BigDecimal> result = service.getBalancesByCurrency();

    assertThat(result).hasSize(3);
    assertThat(result.get(CurrencyEnum.USD)).isEqualByComparingTo("100.00");
    assertThat(result.get(CurrencyEnum.EUR)).isEqualByComparingTo("75.00");
    assertThat(result.get(CurrencyEnum.ARS)).isEqualByComparingTo("500.00");
  }

  @Test
  void getBalancesByCurrency_doesNotDelegateToTheTransactionRepository() {
    createService();
    when(accountRepository.findActive()).thenReturn(List.of(account(1L, CurrencyEnum.USD, "10.00")));

    service.getBalancesByCurrency();

    verifyNoInteractions(transactionRepository);
  }

  // --- getBalancesByCurrency(year) / (year, month) - unchanged regression coverage ---------------------------------

  @Test
  void getBalancesByCurrencyForYear_stillDelegatesToTheTransactionRepository_andNeverTouchesAccountRepository() {
    createService();
    List<Object[]> repositoryResult = List.<Object[]>of(new Object[] {CurrencyEnum.USD, new BigDecimal("42.00")});
    when(transactionRepository.getBalancesByCurrency(2024)).thenReturn(repositoryResult);

    Map<CurrencyEnum, BigDecimal> result = service.getBalancesByCurrency(2024);

    assertThat(result.get(CurrencyEnum.USD)).isEqualByComparingTo("42.00");
    verify(transactionRepository).getBalancesByCurrency(2024);
    verifyNoInteractions(accountRepository);
  }

  @Test
  void getBalancesByCurrencyForYearAndMonth_stillDelegatesToTheTransactionRepository_andNeverTouchesAccountRepository() {
    createService();
    List<Object[]> repositoryResult = List.<Object[]>of(new Object[] {CurrencyEnum.EUR, new BigDecimal("15.00")});
    when(transactionRepository.getBalancesByCurrency(2024, 3)).thenReturn(repositoryResult);

    Map<CurrencyEnum, BigDecimal> result = service.getBalancesByCurrency(2024, 3);

    assertThat(result.get(CurrencyEnum.EUR)).isEqualByComparingTo("15.00");
    verify(transactionRepository).getBalancesByCurrency(2024, 3);
    verifyNoInteractions(accountRepository);
  }
}
