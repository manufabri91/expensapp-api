package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.modelmapper.ModelMapper;
import com.manuelfabri.expenses.dto.ProgrammedTransactionsDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionGroupDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionItemDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionsResponseDto;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.SourceTypeEnum;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.service.RecurrenceDateCalculator;

@ExtendWith(MockitoExtension.class)
class UpcomingTransactionServiceImplementationTest {

  @Mock
  private RecurrentTransactionRepository recurrentTransactionRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private RecurrenceDateCalculator dateCalculator;

  private UpcomingTransactionServiceImplementation service;
  private User currentUser;
  private Account usdAccount;
  private Account eurAccount;
  private Category expenseCategory;
  private Category incomeCategory;
  private LocalDate today;
  private LocalDate endOfMonth;

  @BeforeEach
  void setUp() {
    service = new UpcomingTransactionServiceImplementation(recurrentTransactionRepository, transactionRepository,
        dateCalculator, new ModelMapper());

    currentUser = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    usdAccount = new Account(10L, "Checking USD", CurrencyEnum.USD, currentUser);
    eurAccount = new Account(11L, "Checking EUR", CurrencyEnum.EUR, currentUser);
    expenseCategory = new Category(20L, "Subscriptions", "icon-expense", "#f00", currentUser, List.of());
    expenseCategory.setType(TransactionTypeEnum.EXPENSE);
    incomeCategory = new Category(21L, "Salary", "icon-income", "#0f0", currentUser, List.of());
    incomeCategory.setType(TransactionTypeEnum.INCOME);

    today = LocalDate.now(ZoneOffset.UTC);
    endOfMonth = YearMonth.from(today).atEndOfMonth();

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private RecurrentTransaction recurrence(Long id, TransactionTypeEnum type, BigDecimal amount, Account account,
      Category category, RecurrenceStatusEnum status) {
    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setId(id);
    recurrence.setType(type);
    recurrence.setAmount(amount);
    recurrence.setDescription("Recurrence " + id);
    recurrence.setAccount(account);
    recurrence.setCategory(category);
    recurrence.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    recurrence.setIntervalDays(30);
    recurrence.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    recurrence.setStatus(status);
    return recurrence;
  }

  private Transaction pendingTransaction(Long id, TransactionTypeEnum type, BigDecimal signedAmount, Account account,
      Category category, OffsetDateTime eventDate) {
    Transaction transaction = new Transaction();
    transaction.setId(id);
    transaction.setType(type);
    transaction.setAmount(signedAmount);
    transaction.setDescription("Transaction " + id);
    transaction.setAccount(account);
    transaction.setCategory(category);
    transaction.setEventDate(eventDate);
    transaction.setPending(true);
    return transaction;
  }

  private void stubNoOneTimeTransactions() {
    when(transactionRepository.findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse(eq(currentUser), any(),
        any())).thenReturn(List.of());
  }

  // ---------------------------------------------------------------------
  // getUpcomingTransactions: ACTIVE-only vs getProgrammedTransactions: ACTIVE+PAUSED
  // ---------------------------------------------------------------------

  @Test
  void getUpcomingTransactions_excludesPausedRecurrences_includesOnlyActive() {
    RecurrentTransaction activeRecurrence =
        recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount, expenseCategory,
            RecurrenceStatusEnum.ACTIVE);
    RecurrentTransaction pausedRecurrence =
        recurrence(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("20.00"), usdAccount, expenseCategory,
            RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(activeRecurrence, pausedRecurrence));
    when(dateCalculator.nextDueDate(activeRecurrence)).thenReturn(Optional.of(today));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).hasSize(1);
    assertThat(result.getExpenses().get(0).getItems()).extracting(UpcomingTransactionItemDto::getSourceId)
        .containsExactly(1L);
  }

  @Test
  void getProgrammedTransactions_includesBothActiveAndPausedRecurrences() {
    RecurrentTransaction activeRecurrence =
        recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount, expenseCategory,
            RecurrenceStatusEnum.ACTIVE);
    RecurrentTransaction pausedRecurrence =
        recurrence(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("20.00"), usdAccount, expenseCategory,
            RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(activeRecurrence, pausedRecurrence));
    when(dateCalculator.nextDueDate(activeRecurrence)).thenReturn(Optional.of(today.plusDays(5)));
    when(dateCalculator.nextDueDate(pausedRecurrence)).thenReturn(Optional.of(today.plusDays(10)));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).extracting(UpcomingTransactionItemDto::getSourceId).containsExactly(1L, 2L);
  }

  // ---------------------------------------------------------------------
  // getUpcomingTransactions: window boundary
  // ---------------------------------------------------------------------

  @Test
  void getUpcomingTransactions_includesRecurrence_whenDueDateIsExactlyToday() {
    RecurrentTransaction dueToday = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(dueToday));
    when(dateCalculator.nextDueDate(dueToday)).thenReturn(Optional.of(today));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).hasSize(1);
  }

  @Test
  void getUpcomingTransactions_includesRecurrence_whenDueDateIsExactlyEndOfMonth() {
    RecurrentTransaction dueAtEndOfMonth = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(dueAtEndOfMonth));
    when(dateCalculator.nextDueDate(dueAtEndOfMonth)).thenReturn(Optional.of(endOfMonth));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).hasSize(1);
  }

  @Test
  void getUpcomingTransactions_excludesRecurrence_whenDueDateIsBeforeToday() {
    RecurrentTransaction overdue = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(overdue));
    when(dateCalculator.nextDueDate(overdue)).thenReturn(Optional.of(today.minusDays(1)));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).isEmpty();
  }

  @Test
  void getUpcomingTransactions_excludesRecurrence_whenDueDateIsAfterEndOfMonth() {
    RecurrentTransaction nextMonth = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(nextMonth));
    when(dateCalculator.nextDueDate(nextMonth)).thenReturn(Optional.of(endOfMonth.plusDays(1)));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).isEmpty();
  }

  @Test
  void getUpcomingTransactions_excludesRecurrence_whenCalculatorReturnsEmpty() {
    RecurrentTransaction expired = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(expired));
    when(dateCalculator.nextDueDate(expired)).thenReturn(Optional.empty());
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).isEmpty();
  }

  // ---------------------------------------------------------------------
  // getProgrammedTransactions: unbounded
  // ---------------------------------------------------------------------

  @Test
  void getProgrammedTransactions_includesRecurrencesDueWellBeyondCurrentMonth() {
    RecurrentTransaction farFuture = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(farFuture));
    when(dateCalculator.nextDueDate(farFuture)).thenReturn(Optional.of(today.plusYears(1)));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).hasSize(1);
  }

  // ---------------------------------------------------------------------
  // Amount signing: fromRecurrence signs, fromTransaction uses as-is
  // ---------------------------------------------------------------------

  @Test
  void getUpcomingTransactions_signsTheRecurrenceAmount_butUsesTheTransactionAmountAsIs() {
    RecurrentTransaction expenseRecurrence = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("50.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(expenseRecurrence));
    when(dateCalculator.nextDueDate(expenseRecurrence)).thenReturn(Optional.of(today));

    OffsetDateTime tomorrow = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    Transaction alreadySignedTransaction =
        pendingTransaction(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("-75.00"), usdAccount, expenseCategory,
            tomorrow);
    when(transactionRepository.findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse(eq(currentUser), any(),
        any())).thenReturn(List.of(alreadySignedTransaction));

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    List<UpcomingTransactionItemDto> items = result.getExpenses().get(0).getItems();
    UpcomingTransactionItemDto recurrenceItem =
        items.stream().filter(item -> item.getSourceType() == SourceTypeEnum.RECURRING).findFirst().orElseThrow();
    UpcomingTransactionItemDto transactionItem =
        items.stream().filter(item -> item.getSourceType() == SourceTypeEnum.ONE_TIME).findFirst().orElseThrow();

    // Recurrence amount is stored unsigned (50.00) and must be re-signed to -50.00 for an EXPENSE.
    assertThat(recurrenceItem.getSignedAmount()).isEqualByComparingTo("-50.00");
    // Transaction amount is already signed at creation time and must be used exactly as stored.
    assertThat(transactionItem.getSignedAmount()).isEqualByComparingTo("-75.00");
  }

  // ---------------------------------------------------------------------
  // Merge / group / sort / total
  // ---------------------------------------------------------------------

  @Test
  void getUpcomingTransactions_groupsByCurrency_sortsWithinGroupByDate_andSumsTotal() {
    RecurrentTransaction usdRecurrence = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(usdRecurrence));
    when(dateCalculator.nextDueDate(usdRecurrence)).thenReturn(Optional.of(today.plusDays(2)));

    Transaction usdTransactionEarlier = pendingTransaction(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("-15.00"),
        usdAccount, expenseCategory, today.atStartOfDay().atOffset(ZoneOffset.UTC));
    Transaction eurTransaction = pendingTransaction(3L, TransactionTypeEnum.EXPENSE, new BigDecimal("-40.00"),
        eurAccount, expenseCategory, today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
    when(transactionRepository.findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse(eq(currentUser), any(),
        any())).thenReturn(List.of(usdTransactionEarlier, eurTransaction));

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).hasSize(2);
    UpcomingTransactionGroupDto usdGroup = result.getExpenses().stream()
        .filter(group -> group.getCurrency().equals("USD")).findFirst().orElseThrow();
    UpcomingTransactionGroupDto eurGroup = result.getExpenses().stream()
        .filter(group -> group.getCurrency().equals("EUR")).findFirst().orElseThrow();

    // USD group: transaction (today) then recurrence (today+2), sorted ascending by date.
    assertThat(usdGroup.getItems()).extracting(UpcomingTransactionItemDto::getSourceId).containsExactly(2L, 1L);
    assertThat(usdGroup.getTotal()).isEqualByComparingTo("-45.00");

    assertThat(eurGroup.getItems()).extracting(UpcomingTransactionItemDto::getSourceId).containsExactly(3L);
    assertThat(eurGroup.getTotal()).isEqualByComparingTo("-40.00");
  }

  @Test
  void getUpcomingTransactions_splitsExpensesAndIncomesIntoSeparateResponseLists() {
    RecurrentTransaction expenseRecurrence = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    RecurrentTransaction incomeRecurrence = recurrence(2L, TransactionTypeEnum.INCOME, new BigDecimal("1000.00"),
        usdAccount, incomeCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(expenseRecurrence, incomeRecurrence));
    when(dateCalculator.nextDueDate(expenseRecurrence)).thenReturn(Optional.of(today));
    when(dateCalculator.nextDueDate(incomeRecurrence)).thenReturn(Optional.of(today));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).hasSize(1);
    assertThat(result.getExpenses().get(0).getItems()).extracting(UpcomingTransactionItemDto::getSourceId)
        .containsExactly(1L);
    assertThat(result.getIncomes()).hasSize(1);
    assertThat(result.getIncomes().get(0).getItems()).extracting(UpcomingTransactionItemDto::getSourceId)
        .containsExactly(2L);
  }

  @Test
  void getProgrammedTransactions_interleavesRecurringAndOneTimeItemsSortedByDate() {
    RecurrentTransaction laterRecurrence = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(laterRecurrence));
    when(dateCalculator.nextDueDate(laterRecurrence)).thenReturn(Optional.of(today.plusDays(10)));

    Transaction earlierTransaction = pendingTransaction(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("-15.00"),
        usdAccount, expenseCategory, today.plusDays(3).atStartOfDay().atOffset(ZoneOffset.UTC));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser))
        .thenReturn(List.of(earlierTransaction));

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).extracting(UpcomingTransactionItemDto::getSourceId).containsExactly(2L, 1L);
  }

  // ---------------------------------------------------------------------
  // Transfers never appear
  // ---------------------------------------------------------------------

  @Test
  void getUpcomingTransactions_excludesTransferTypeRecurrences_fromBothBuckets() {
    RecurrentTransaction transferRecurrence = recurrence(1L, TransactionTypeEnum.TRANSFER, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(transferRecurrence));
    when(dateCalculator.nextDueDate(transferRecurrence)).thenReturn(Optional.of(today));
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).isEmpty();
    assertThat(result.getIncomes()).isEmpty();
  }

  @Test
  void getProgrammedTransactions_excludesTransferTypeRecurrences_fromBothBuckets() {
    RecurrentTransaction transferRecurrence = recurrence(1L, TransactionTypeEnum.TRANSFER, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(transferRecurrence));
    when(dateCalculator.nextDueDate(transferRecurrence)).thenReturn(Optional.of(today));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).isEmpty();
    assertThat(result.getIncomes()).isEmpty();
  }

  @Test
  void getProgrammedTransactions_excludesCancelledRecurrences_evenIfRepositoryReturnsOne() {
    RecurrentTransaction cancelledRecurrence = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.CANCELLED);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(cancelledRecurrence));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).isEmpty();
  }

  // ---------------------------------------------------------------------
  // getProgrammedTransactions: ended recurrences (ACTIVE/PAUSED, no more due dates) are still included
  // ---------------------------------------------------------------------

  @Test
  void getProgrammedTransactions_includesEndedActiveRecurrence_withNullDate() {
    RecurrentTransaction endedActive = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(endedActive));
    when(dateCalculator.nextDueDate(endedActive)).thenReturn(Optional.empty());
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).hasSize(1);
    assertThat(result.getExpenses().get(0).getSourceId()).isEqualTo(1L);
    assertThat(result.getExpenses().get(0).getDate()).isNull();
  }

  @Test
  void getProgrammedTransactions_includesEndedPausedRecurrence_withNullDate() {
    RecurrentTransaction endedPaused = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"),
        usdAccount, expenseCategory, RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(endedPaused));
    when(dateCalculator.nextDueDate(endedPaused)).thenReturn(Optional.empty());
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).hasSize(1);
    assertThat(result.getExpenses().get(0).getSourceId()).isEqualTo(1L);
    assertThat(result.getExpenses().get(0).getDate()).isNull();
  }

  @Test
  void getUpcomingTransactions_stillExcludesEndedRecurrence_regressionForBoundedDashboardView() {
    RecurrentTransaction ended = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("30.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(ended));
    when(dateCalculator.nextDueDate(ended)).thenReturn(Optional.empty());
    stubNoOneTimeTransactions();

    UpcomingTransactionsResponseDto result = service.getUpcomingTransactions();

    assertThat(result.getExpenses()).isEmpty();
  }

  @Test
  void getProgrammedTransactions_sortsNullDatedEndedRecurrences_last() {
    RecurrentTransaction dated = recurrence(1L, TransactionTypeEnum.EXPENSE, new BigDecimal("10.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    RecurrentTransaction ended = recurrence(2L, TransactionTypeEnum.EXPENSE, new BigDecimal("20.00"), usdAccount,
        expenseCategory, RecurrenceStatusEnum.ACTIVE);
    // Repository order deliberately puts the ended (null-date) recurrence before the dated one, so the assertion
    // proves the sort — not incidental insertion order — puts it last.
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(ended, dated));
    when(dateCalculator.nextDueDate(ended)).thenReturn(Optional.empty());
    when(dateCalculator.nextDueDate(dated)).thenReturn(Optional.of(today.plusDays(5)));
    when(transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(currentUser)).thenReturn(List.of());

    ProgrammedTransactionsDto result = service.getProgrammedTransactions();

    assertThat(result.getExpenses()).extracting(UpcomingTransactionItemDto::getSourceId).containsExactly(1L, 2L);
    assertThat(result.getExpenses().get(1).getDate()).isNull();
  }
}
