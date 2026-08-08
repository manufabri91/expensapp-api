package com.manuelfabri.expenses.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;

/**
 * Note: assertions on the effect of {@code activateDuePendingTransactions} deliberately read back the
 * {@code pending}/{@code excludeFromTotals} columns via a scalar JPQL projection (see
 * {@link #currentPendingState(Long)}) rather than re-loading the {@code Transaction} entity itself, to keep this
 * test decoupled from the {@code Account} association and its {@code accountBalance} {@code @Formula} (covered
 * separately by {@code AccountRepositoryTest}). {@code Account.initialBalance} and {@code Transaction.excludeFromTotals}
 * now carry explicit {@code @Column(name = ...)} mappings that match the Liquibase-managed physical column names
 * ("initialbalance", "excludefromtotals") exactly, resolving what used to be a mismatch against the snake_case
 * names this test's {@code ddl-auto: create-drop} schema previously generated for those two fields.
 */
@DataJpaTest
class TransactionRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private TransactionRepository transactionRepository;

  private User owner;
  private Account account;
  private Category category;
  private Subcategory subcategory;

  @BeforeEach
  void setUp() {
    owner = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    entityManager.persist(owner);
    account = new Account(null, "Checking", CurrencyEnum.USD, owner);
    entityManager.persist(account);
    category = new Category(null, "Groceries", "icon", "#fff", owner, List.of());
    category.setType(TransactionTypeEnum.EXPENSE);
    entityManager.persist(category);
    subcategory = new Subcategory(null, "Supermarket", category, List.of(), owner);
    entityManager.persist(subcategory);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(owner, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private Transaction persistTransaction(OffsetDateTime eventDate, boolean pending, boolean excludeFromTotals) {
    return persistTransaction(eventDate, pending, excludeFromTotals, new BigDecimal("-10.00"), false);
  }

  private Transaction persistTransaction(OffsetDateTime eventDate, boolean pending, boolean excludeFromTotals,
      BigDecimal amount, boolean deleted) {
    Transaction transaction = new Transaction();
    transaction.setOwner(owner);
    transaction.setType(amount.signum() >= 0 ? TransactionTypeEnum.INCOME : TransactionTypeEnum.EXPENSE);
    transaction.setAmount(amount);
    transaction.setDescription("Test transaction");
    transaction.setEventDate(eventDate);
    transaction.setAccount(account);
    transaction.setCategory(category);
    transaction.setSubcategory(subcategory);
    transaction.setPending(pending);
    transaction.setExcludeFromTotals(excludeFromTotals);
    Transaction persisted = entityManager.persistAndFlush(transaction);
    if (deleted) {
      transactionRepository.softDeleteById(persisted.getId());
      entityManager.flush();
    }
    return persisted;
  }

  private Object[] currentPendingState(Long transactionId) {
    return (Object[]) entityManager.getEntityManager()
        .createQuery("select t.pending, t.excludeFromTotals from transactions t where t.id = :id")
        .setParameter("id", transactionId).getSingleResult();
  }

  // --- activateDuePendingTransactions ---------------------------------------------------------------------------

  @Test
  void activateDuePendingTransactions_activatesADuePendingTransaction() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    Transaction duePending =
        persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);

    transactionRepository.activateDuePendingTransactions(asOf);

    Object[] state = currentPendingState(duePending.getId());
    assertThat((Boolean) state[0]).isFalse();
    assertThat((Boolean) state[1]).isFalse();
  }

  @Test
  void activateDuePendingTransactions_leavesAPermanentlyExcludedTransactionUntouched() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    Transaction permanentlyExcluded =
        persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), false, true);

    transactionRepository.activateDuePendingTransactions(asOf);

    Object[] state = currentPendingState(permanentlyExcluded.getId());
    assertThat((Boolean) state[0]).isFalse();
    assertThat((Boolean) state[1]).isTrue();
  }

  @Test
  void activateDuePendingTransactions_leavesAFuturePendingTransactionUntouched() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    Transaction futurePending =
        persistTransaction(OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);

    transactionRepository.activateDuePendingTransactions(asOf);

    Object[] state = currentPendingState(futurePending.getId());
    assertThat((Boolean) state[0]).isTrue();
    assertThat((Boolean) state[1]).isTrue();
  }

  @Test
  void activateDuePendingTransactions_leavesASoftDeletedDuePendingTransactionUntouched() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    Transaction softDeletedDuePending =
        persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    transactionRepository.softDeleteById(softDeletedDuePending.getId());
    entityManager.flush();

    transactionRepository.activateDuePendingTransactions(asOf);

    Object[] state = currentPendingState(softDeletedDuePending.getId());
    assertThat((Boolean) state[0]).isTrue();
    assertThat((Boolean) state[1]).isTrue();
  }

  // --- findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse ----------------------------------------------

  @Test
  void findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse_returnsOnlyPendingTransactionsInRange() {
    OffsetDateTime rangeStart = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime rangeEnd = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);

    Transaction pendingInRange =
        persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    persistTransaction(OffsetDateTime.of(2024, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC), true, true); // pending, out of range
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), false, false); // not pending
    Transaction softDeletedPendingInRange =
        persistTransaction(OffsetDateTime.of(2024, 1, 20, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    transactionRepository.softDeleteById(softDeletedPendingInRange.getId());
    entityManager.flush();

    List<Transaction> result =
        transactionRepository.findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse(owner, rangeStart, rangeEnd);

    assertThat(result).extracting(Transaction::getId).containsExactly(pendingInRange.getId());
  }

  // --- findByOwnerAndPendingTrueAndDeletedFalse -----------------------------------------------------------------

  @Test
  void findByOwnerAndPendingTrueAndDeletedFalse_returnsAllPendingUndeletedTransactionsRegardlessOfDate() {
    Transaction pendingPast =
        persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    Transaction pendingFuture =
        persistTransaction(OffsetDateTime.of(2024, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), false, false); // not pending
    Transaction softDeletedPending =
        persistTransaction(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true);
    transactionRepository.softDeleteById(softDeletedPending.getId());
    entityManager.flush();

    List<Transaction> result = transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(owner);

    assertThat(result).extracting(Transaction::getId).containsExactlyInAnyOrder(pendingPast.getId(),
        pendingFuture.getId());
  }

  // --- getTransactionsTotalIncomesByMonth(IncludingPending) / getTransactionsTotalExpensesByMonth(IncludingPending) --

  @Test
  void getTransactionsTotalIncomesByMonthIncludingPending_includesAPendingTransactionThatTheExistingMethodExcludes() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("100.00"),
        false);

    List<Object[]> existingResult = transactionRepository.getTransactionsTotalIncomesByMonth(fromDate);
    List<Object[]> includingPendingResult =
        transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate);

    assertThat(existingResult).isEmpty();
    assertThat(includingPendingResult).hasSize(1);
    assertThat((BigDecimal) includingPendingResult.get(0)[3]).isEqualByComparingTo("100.00");
  }

  @Test
  void getTransactionsTotalExpensesByMonthIncludingPending_includesAPendingTransactionThatTheExistingMethodExcludes() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("-50.00"),
        false);

    List<Object[]> existingResult = transactionRepository.getTransactionsTotalExpensesByMonth(fromDate);
    List<Object[]> includingPendingResult =
        transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate);

    assertThat(existingResult).isEmpty();
    assertThat(includingPendingResult).hasSize(1);
    assertThat((BigDecimal) includingPendingResult.get(0)[3]).isEqualByComparingTo("-50.00");
  }

  @Test
  void getTransactionsTotalIncomesByMonthAndIncludingPendingVariant_bothExcludeATransferLeg() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), false, true, new BigDecimal("75.00"),
        false); // transfer leg: excludeFromTotals=true, pending=false

    assertThat(transactionRepository.getTransactionsTotalIncomesByMonth(fromDate)).isEmpty();
    assertThat(transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  @Test
  void getTransactionsTotalExpensesByMonthAndIncludingPendingVariant_bothExcludeATransferLeg() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), false, true, new BigDecimal("-75.00"),
        false); // transfer leg: excludeFromTotals=true, pending=false

    assertThat(transactionRepository.getTransactionsTotalExpensesByMonth(fromDate)).isEmpty();
    assertThat(transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  @Test
  void getTransactionsTotalIncomesByMonthAndIncludingPendingVariant_bothExcludeADeletedTransaction() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), false, false, new BigDecimal("60.00"),
        true); // deleted, would otherwise be a normal counted income

    assertThat(transactionRepository.getTransactionsTotalIncomesByMonth(fromDate)).isEmpty();
    assertThat(transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  @Test
  void getTransactionsTotalExpensesByMonthAndIncludingPendingVariant_bothExcludeADeletedTransaction() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC), false, false, new BigDecimal("-60.00"),
        true); // deleted, would otherwise be a normal counted expense

    assertThat(transactionRepository.getTransactionsTotalExpensesByMonth(fromDate)).isEmpty();
    assertThat(transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  // --- getTransactionsTotal{Incomes,Expenses}ByMonthIncludingPending: new upper bound (toDate) clamping ------------

  @Test
  void getTransactionsTotalIncomesByMonthIncludingPending_excludesAPendingTransactionDatedAfterToDate() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("100.00"),
        false); // pending, dated next month - beyond toDate

    assertThat(transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  @Test
  void getTransactionsTotalExpensesByMonthIncludingPending_excludesAPendingTransactionDatedAfterToDate() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("-100.00"),
        false); // pending, dated next month - beyond toDate

    assertThat(transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate)).isEmpty();
  }

  @Test
  void getTransactionsTotalIncomesByMonthIncludingPending_includesAPendingTransactionDatedWithinTheCurrentMonth() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 20, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("100.00"),
        false); // pending, dated within the current month - within bounds

    List<Object[]> result = transactionRepository.getTransactionsTotalIncomesByMonthIncludingPending(fromDate, toDate);

    assertThat(result).hasSize(1);
    assertThat((BigDecimal) result.get(0)[3]).isEqualByComparingTo("100.00");
  }

  @Test
  void getTransactionsTotalExpensesByMonthIncludingPending_includesAPendingTransactionDatedWithinTheCurrentMonth() {
    OffsetDateTime fromDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime toDate = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    persistTransaction(OffsetDateTime.of(2024, 1, 20, 0, 0, 0, 0, ZoneOffset.UTC), true, true, new BigDecimal("-100.00"),
        false); // pending, dated within the current month - within bounds

    List<Object[]> result =
        transactionRepository.getTransactionsTotalExpensesByMonthIncludingPending(fromDate, toDate);

    assertThat(result).hasSize(1);
    assertThat((BigDecimal) result.get(0)[3]).isEqualByComparingTo("-100.00");
  }
}
