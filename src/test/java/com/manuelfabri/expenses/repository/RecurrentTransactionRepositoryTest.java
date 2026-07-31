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
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;

@DataJpaTest
class RecurrentTransactionRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private RecurrentTransactionRepository recurrentTransactionRepository;

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
    category = new Category(null, "Subscriptions", "icon", "#fff", owner, List.of());
    category.setType(TransactionTypeEnum.EXPENSE);
    entityManager.persist(category);
    subcategory = new Subcategory(null, "Streaming", category, List.of(), owner);
    entityManager.persist(subcategory);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(owner, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private RecurrentTransaction persistRecurrence(RecurrenceStatusEnum status, OffsetDateTime startDate,
      OffsetDateTime endDate) {
    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setOwner(owner);
    recurrence.setType(TransactionTypeEnum.EXPENSE);
    recurrence.setAmount(new BigDecimal("9.99"));
    recurrence.setDescription("Streaming subscription");
    recurrence.setAccount(account);
    recurrence.setCategory(category);
    recurrence.setSubcategory(subcategory);
    recurrence.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    recurrence.setIntervalDays(30);
    recurrence.setStartDate(startDate);
    recurrence.setEndDate(endDate);
    recurrence.setStatus(status);
    return entityManager.persistAndFlush(recurrence);
  }

  @Test
  void findDueForGeneration_returnsOnlyActiveRecurrencesThatHaveStartedAndNotEnded() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    RecurrentTransaction dueRecurrence =
        persistRecurrence(RecurrenceStatusEnum.ACTIVE, OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null);
    persistRecurrence(RecurrenceStatusEnum.PAUSED, OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null);
    persistRecurrence(RecurrenceStatusEnum.ACTIVE, OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC), null);
    persistRecurrence(RecurrenceStatusEnum.ACTIVE, OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC));

    List<RecurrentTransaction> due = recurrentTransactionRepository.findDueForGeneration(asOf);

    assertThat(due).extracting(RecurrentTransaction::getId).containsExactly(dueRecurrence.getId());
  }

  @Test
  void findDueForGeneration_includesARecurrenceEndingExactlyOnTheAsOfDate() {
    OffsetDateTime asOf = OffsetDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);
    RecurrentTransaction endingTodayRecurrence = persistRecurrence(RecurrenceStatusEnum.ACTIVE,
        OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), asOf);

    List<RecurrentTransaction> due = recurrentTransactionRepository.findDueForGeneration(asOf);

    assertThat(due).extracting(RecurrentTransaction::getId).containsExactly(endingTodayRecurrence.getId());
  }

  @Test
  void findActiveVisible_excludesCancelledRecurrences_butIncludesPausedOnes() {
    RecurrentTransaction activeRecurrence =
        persistRecurrence(RecurrenceStatusEnum.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC), null);
    RecurrentTransaction pausedRecurrence =
        persistRecurrence(RecurrenceStatusEnum.PAUSED, OffsetDateTime.now(ZoneOffset.UTC), null);
    persistRecurrence(RecurrenceStatusEnum.CANCELLED, OffsetDateTime.now(ZoneOffset.UTC), null);

    List<RecurrentTransaction> visible = recurrentTransactionRepository.findActiveVisible();

    assertThat(visible).extracting(RecurrentTransaction::getId).containsExactlyInAnyOrder(activeRecurrence.getId(),
        pausedRecurrence.getId());
  }

  @Test
  void findActiveVisible_excludesSoftDeletedRecurrences() {
    RecurrentTransaction deletedRecurrence =
        persistRecurrence(RecurrenceStatusEnum.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC), null);
    recurrentTransactionRepository.softDeleteById(deletedRecurrence.getId());
    entityManager.flush();
    entityManager.clear();

    List<RecurrentTransaction> visible = recurrentTransactionRepository.findActiveVisible();

    assertThat(visible).isEmpty();
  }
}
