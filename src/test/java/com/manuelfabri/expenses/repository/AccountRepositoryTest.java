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
 * Covers {@link Account#getAccountBalance()}, the Hibernate {@code @Formula}-computed running balance. The formula
 * is raw native SQL against the real column names, so every assertion here reloads the account from a cleared
 * persistence context ({@link #reloadAccount()}) to force Hibernate to re-evaluate the formula rather than reuse the
 * managed instance created by {@code persist}.
 */
@DataJpaTest
class AccountRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private AccountRepository accountRepository;
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

  private Account persistAccount(BigDecimal initialBalance) {
    Account newAccount = new Account(null, "Checking", CurrencyEnum.USD, owner);
    newAccount.setInitialBalance(initialBalance);
    return entityManager.persistAndFlush(newAccount);
  }

  private Transaction persistTransaction(Account forAccount, BigDecimal amount, boolean excludeFromTotals) {
    return persistTransaction(forAccount, amount, excludeFromTotals, false);
  }

  private Transaction persistTransaction(Account forAccount, BigDecimal amount, boolean excludeFromTotals,
      boolean pending) {
    Transaction transaction = new Transaction();
    transaction.setOwner(owner);
    transaction.setType(TransactionTypeEnum.EXPENSE);
    transaction.setAmount(amount);
    transaction.setDescription("Test transaction");
    transaction.setEventDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    transaction.setAccount(forAccount);
    transaction.setCategory(category);
    transaction.setSubcategory(subcategory);
    transaction.setExcludeFromTotals(excludeFromTotals);
    transaction.setPending(pending);
    return entityManager.persistAndFlush(transaction);
  }

  private Account reloadAccount() {
    entityManager.clear();
    return accountRepository.findActiveById(account.getId()).orElseThrow();
  }

  // --- accountBalance formula ------------------------------------------------------------------------------------

  @Test
  void accountBalance_includesInitialBalancePlusNormalTransactions() {
    account = persistAccount(new BigDecimal("100.00"));
    persistTransaction(account, new BigDecimal("50.00"), false);
    persistTransaction(account, new BigDecimal("-20.00"), false);

    Account reloaded = reloadAccount();

    assertThat(reloaded.getAccountBalance()).isEqualByComparingTo("130.00");
  }

  @Test
  void accountBalance_excludesASoftDeletedTransaction() {
    account = persistAccount(new BigDecimal("100.00"));
    persistTransaction(account, new BigDecimal("-20.00"), false);
    Transaction softDeleted = persistTransaction(account, new BigDecimal("-1000.00"), false);
    transactionRepository.softDeleteById(softDeleted.getId());
    entityManager.flush();

    Account reloaded = reloadAccount();

    assertThat(reloaded.getAccountBalance()).isEqualByComparingTo("80.00");
  }

  @Test
  void accountBalance_includesATransferLegShapedExcludeFromTotalsTransaction() {
    // Transfer legs are written with excludeFromTotals = true but pending = false: the money genuinely moved,
    // so it must still count toward the account's balance.
    account = persistAccount(new BigDecimal("100.00"));
    persistTransaction(account, new BigDecimal("-20.00"), false);
    persistTransaction(account, new BigDecimal("-50.00"), true, false);

    Account reloaded = reloadAccount();

    assertThat(reloaded.getAccountBalance()).isEqualByComparingTo("30.00");
  }

  @Test
  void accountBalance_excludesAPendingTransaction() {
    // Pending transactions haven't happened yet (money hasn't moved), so they must not count until their date
    // arrives and activateDuePendingTransactions clears the pending flag.
    account = persistAccount(new BigDecimal("100.00"));
    persistTransaction(account, new BigDecimal("-20.00"), false);
    persistTransaction(account, new BigDecimal("-50.00"), true, true);

    Account reloaded = reloadAccount();

    assertThat(reloaded.getAccountBalance()).isEqualByComparingTo("80.00");
  }

  @Test
  void accountBalance_withNoTransactions_equalsInitialBalance() {
    account = persistAccount(new BigDecimal("250.00"));

    Account reloaded = reloadAccount();

    assertThat(reloaded.getAccountBalance()).isEqualByComparingTo("250.00");
  }
}
