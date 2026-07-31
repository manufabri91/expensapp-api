package com.manuelfabri.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class RecurrentTransactionGeneratorServiceTest {

  @Mock
  private RecurrentTransactionRepository recurrentTransactionRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private RecurrenceDateCalculator dateCalculator;

  private RecurrentTransactionGeneratorService generatorService;

  @BeforeEach
  void setUp() {
    generatorService =
        new RecurrentTransactionGeneratorService(recurrentTransactionRepository, transactionRepository, dateCalculator);
  }

  private RecurrentTransaction subscriptionRecurrence() {
    Account account = new Account();
    account.setId(10L);
    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setId(1L);
    recurrence.setType(TransactionTypeEnum.EXPENSE);
    recurrence.setAmount(new BigDecimal("9.99"));
    recurrence.setDescription("Streaming subscription");
    recurrence.setAccount(account);
    recurrence.setExcludeFromTotals(false);
    return recurrence;
  }

  @Test
  void generateForRecurrence_doesNothing_whenNoOccurrencesAreDue() {
    RecurrentTransaction recurrence = subscriptionRecurrence();
    LocalDate asOf = LocalDate.of(2024, 2, 1);
    when(dateCalculator.occurrencesToGenerate(recurrence, asOf)).thenReturn(List.of());

    generatorService.generateForRecurrence(recurrence, asOf);

    verify(transactionRepository, never()).save(any());
    verify(recurrentTransactionRepository, never()).save(any());
  }

  @Test
  void generateForRecurrence_savesOneTransactionPerDueOccurrence_andAdvancesLastGeneratedDate() {
    RecurrentTransaction recurrence = subscriptionRecurrence();
    LocalDate asOf = LocalDate.of(2024, 1, 14);
    List<LocalDate> missedOccurrences = List.of(LocalDate.of(2024, 1, 11), LocalDate.of(2024, 1, 12),
        LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 14));
    when(dateCalculator.occurrencesToGenerate(recurrence, asOf)).thenReturn(missedOccurrences);

    generatorService.generateForRecurrence(recurrence, asOf);

    verify(transactionRepository, times(4)).save(any(Transaction.class));
    assertThat(recurrence.getLastGeneratedDate())
        .isEqualTo(OffsetDateTime.of(2024, 1, 14, 0, 0, 0, 0, ZoneOffset.UTC));
    verify(recurrentTransactionRepository).save(recurrence);
  }

  @Test
  void generateForRecurrence_buildsTransaction_withSignAppliedAndFieldsCopiedFromRecurrence() {
    RecurrentTransaction recurrence = subscriptionRecurrence();
    LocalDate asOf = LocalDate.of(2024, 1, 1);
    when(dateCalculator.occurrencesToGenerate(recurrence, asOf)).thenReturn(List.of(asOf));

    generatorService.generateForRecurrence(recurrence, asOf);

    ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction generatedTransaction = transactionCaptor.getValue();
    assertThat(generatedTransaction.getAmount()).isEqualByComparingTo("-9.99");
    assertThat(generatedTransaction.getType()).isEqualTo(TransactionTypeEnum.EXPENSE);
    assertThat(generatedTransaction.getDescription()).isEqualTo("Streaming subscription");
    assertThat(generatedTransaction.getAccount()).isEqualTo(recurrence.getAccount());
    assertThat(generatedTransaction.getEventDate()).isEqualTo(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    assertThat(generatedTransaction.getExcludeFromTotals()).isFalse();
  }

  @Test
  void generateDueTransactions_processesEveryDueRecurrenceFoundByTheRepository() {
    RecurrentTransaction firstRecurrence = subscriptionRecurrence();
    RecurrentTransaction secondRecurrence = subscriptionRecurrence();
    secondRecurrence.setId(2L);
    when(recurrentTransactionRepository.findDueForGeneration(any())).thenReturn(List.of(firstRecurrence, secondRecurrence));
    when(dateCalculator.occurrencesToGenerate(eq(firstRecurrence), any())).thenReturn(List.of(LocalDate.of(2024, 1, 1)));
    when(dateCalculator.occurrencesToGenerate(eq(secondRecurrence), any())).thenReturn(List.of(LocalDate.of(2024, 1, 1)));

    generatorService.generateDueTransactions();

    verify(transactionRepository, times(2)).save(any(Transaction.class));
    verify(recurrentTransactionRepository).save(firstRecurrence);
    verify(recurrentTransactionRepository).save(secondRecurrence);
  }

  @Test
  void generateDueTransactions_isolatesFailures_soOneBadRecurrenceDoesNotBlockTheRest() {
    RecurrentTransaction failingRecurrence = subscriptionRecurrence();
    RecurrentTransaction healthyRecurrence = subscriptionRecurrence();
    healthyRecurrence.setId(2L);
    when(recurrentTransactionRepository.findDueForGeneration(any()))
        .thenReturn(List.of(failingRecurrence, healthyRecurrence));
    when(dateCalculator.occurrencesToGenerate(eq(failingRecurrence), any()))
        .thenThrow(new RuntimeException("boom"));
    when(dateCalculator.occurrencesToGenerate(eq(healthyRecurrence), any())).thenReturn(List.of(LocalDate.of(2024, 1, 1)));

    generatorService.generateDueTransactions();

    verify(transactionRepository, times(1)).save(any(Transaction.class));
    verify(recurrentTransactionRepository).save(healthyRecurrence);
    verify(recurrentTransactionRepository, never()).save(failingRecurrence);
  }
}
