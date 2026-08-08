package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manuelfabri.expenses.dto.MonthlyBalanceSummaryDto;
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

  // --- getMonthlyHistory(months, includePending) ------------------------------------------------------------------

  @Test
  void getMonthlyHistory_withIncludePendingTrue_includesAPendingTransactionsAmountInTheCurrentMonthsBucket() {
    createService();
    OffsetDateTime now = OffsetDateTime.now();
    Object[] currentMonthIncomeRow =
        new Object[] {now.getYear(), now.getMonthValue(), CurrencyEnum.USD, new BigDecimal("120.00")};

    when(transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(any(OffsetDateTime.class)))
        .thenReturn(List.<Object[]>of(currentMonthIncomeRow));
    when(transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(any(OffsetDateTime.class)))
        .thenReturn(List.<Object[]>of());

    List<MonthlyBalanceSummaryDto> result = service.getMonthlyHistory(1, true);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getYear()).isEqualTo(now.getYear());
    assertThat(result.get(0).getMonth()).isEqualTo(now.getMonthValue());
    assertThat(result.get(0).getIncomes()).isEqualByComparingTo("120.00");
    verify(transactionRepository).getTransactionsTotalIncomesByMonthIncludingPending(any(OffsetDateTime.class));
    verify(transactionRepository, never()).getTransactionsTotalIncomesByMonth(any(OffsetDateTime.class));
  }

  @Test
  void getMonthlyHistory_withIncludePendingFalse_andTheLegacyOverload_produceIdenticalResults_excludingThePendingAmount() {
    createService();
    List<Object[]> incomeResults =
        List.<Object[]>of(new Object[] {2026, 8, CurrencyEnum.USD, new BigDecimal("300.00")});
    List<Object[]> expenseResults =
        List.<Object[]>of(new Object[] {2026, 8, CurrencyEnum.USD, new BigDecimal("-45.00")});
    when(transactionRepository.getTransactionsTotalIncomesByMonth(any(OffsetDateTime.class))).thenReturn(incomeResults);
    when(transactionRepository.getTransactionsTotalExpensesByMonth(any(OffsetDateTime.class)))
        .thenReturn(expenseResults);

    List<MonthlyBalanceSummaryDto> explicitFalseResult = service.getMonthlyHistory(3, false);
    List<MonthlyBalanceSummaryDto> legacyResult = service.getMonthlyHistory(3);

    assertThat(legacyResult).hasSize(1);
    assertThat(explicitFalseResult).hasSize(1);
    assertThat(legacyResult.get(0).getYear()).isEqualTo(explicitFalseResult.get(0).getYear());
    assertThat(legacyResult.get(0).getMonth()).isEqualTo(explicitFalseResult.get(0).getMonth());
    assertThat(legacyResult.get(0).getIncomes()).isEqualByComparingTo(explicitFalseResult.get(0).getIncomes());
    assertThat(legacyResult.get(0).getExpenses()).isEqualByComparingTo(explicitFalseResult.get(0).getExpenses());
    assertThat(explicitFalseResult.get(0).getIncomes()).isEqualByComparingTo("300.00"); // the pending amount never
                                                                                          // surfaces here
    verify(transactionRepository, never()).getTransactionsTotalIncomesByMonthIncludingPending(any(OffsetDateTime.class));
    verify(transactionRepository, never())
        .getTransactionsTotalExpensesByMonthIncludingPending(any(OffsetDateTime.class));
  }

  @Test
  void getMonthlyHistory_withIncludePendingTrue_aRowForALaterMonthNeverAltersAnEarlierRequestedMonthsBucket() {
    // Sanity check on the "no upper bound needed" reasoning behind the *IncludingPending repository queries:
    // because those queries only add a lower bound (fromDate) and pending transactions are always dated "now" or
    // later, a pending transaction due in a month beyond the current one can only ever create its OWN year/month
    // bucket - it can never retroactively change the totals already computed for an earlier, in-range month.
    createService();
    OffsetDateTime now = OffsetDateTime.now();
    int laterYear = now.getMonthValue() == 12 ? now.getYear() + 1 : now.getYear();
    int laterMonth = now.getMonthValue() == 12 ? 1 : now.getMonthValue() + 1;

    Object[] currentMonthRow = new Object[] {now.getYear(), now.getMonthValue(), CurrencyEnum.USD, new BigDecimal("200.00")};
    Object[] laterMonthRow = new Object[] {laterYear, laterMonth, CurrencyEnum.USD, new BigDecimal("999.00")};

    when(transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(any(OffsetDateTime.class)))
        .thenReturn(List.<Object[]>of(currentMonthRow, laterMonthRow));
    when(transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(any(OffsetDateTime.class)))
        .thenReturn(List.of());

    List<MonthlyBalanceSummaryDto> result = service.getMonthlyHistory(3, true);

    assertThat(result).hasSize(2);
    MonthlyBalanceSummaryDto currentBucket = result.stream()
        .filter(dto -> dto.getMonth() == now.getMonthValue() && dto.getYear() == now.getYear()).findFirst()
        .orElseThrow();
    MonthlyBalanceSummaryDto laterBucket = result.stream()
        .filter(dto -> dto.getMonth() == laterMonth && dto.getYear() == laterYear).findFirst().orElseThrow();
    assertThat(currentBucket.getIncomes()).isEqualByComparingTo("200.00");
    assertThat(laterBucket.getIncomes()).isEqualByComparingTo("999.00");
  }
}
