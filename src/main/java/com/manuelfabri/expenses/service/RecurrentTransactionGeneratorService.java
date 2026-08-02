package com.manuelfabri.expenses.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;

/**
 * Turns due {@link RecurrentTransaction} definitions into real {@link Transaction} rows once a day. Runs with no
 * authenticated principal (there is no logged-in user during a cron trigger), so it saves through the repository
 * directly rather than {@code TransactionServiceImplementation}, which assumes one.
 */
@Service
public class RecurrentTransactionGeneratorService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecurrentTransactionGeneratorService.class);

  private RecurrentTransactionRepository recurrentTransactionRepository;
  private TransactionRepository transactionRepository;
  private RecurrenceDateCalculator dateCalculator;

  public RecurrentTransactionGeneratorService(RecurrentTransactionRepository recurrentTransactionRepository,
      TransactionRepository transactionRepository, RecurrenceDateCalculator dateCalculator) {
    this.recurrentTransactionRepository = recurrentTransactionRepository;
    this.transactionRepository = transactionRepository;
    this.dateCalculator = dateCalculator;
  }

  // TODO: add a distributed lock (e.g. ShedLock) if this app is ever deployed across more than one instance.
  @Scheduled(cron = "0 5 0 * * *")
  public void generateDueTransactions() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<RecurrentTransaction> due = recurrentTransactionRepository.findDueForGeneration(now);

    for (RecurrentTransaction recurrence : due) {
      try {
        generateForRecurrence(recurrence, now.toLocalDate());
      } catch (Exception e) {
        LOGGER.error("Failed to generate transactions for recurrence {}", recurrence.getId(), e);
      }
    }
  }

  @Transactional
  public void generateForRecurrence(RecurrentTransaction recurrence, LocalDate asOf) {
    List<LocalDate> occurrences = dateCalculator.occurrencesToGenerate(recurrence, asOf);
    if (occurrences.isEmpty()) {
      return;
    }

    for (LocalDate occurrence : occurrences) {
      transactionRepository.save(buildTransaction(recurrence, occurrence));
    }

    recurrence.setLastGeneratedDate(occurrences.get(occurrences.size() - 1).atStartOfDay().atOffset(ZoneOffset.UTC));
    recurrentTransactionRepository.save(recurrence);
  }

  private Transaction buildTransaction(RecurrentTransaction recurrence, LocalDate eventDate) {
    Transaction transaction = new Transaction();
    transaction.setOwner(recurrence.getOwner());
    transaction.setType(recurrence.getType());
    transaction.setEventDate(eventDate.atStartOfDay().atOffset(ZoneOffset.UTC));
    transaction.setDescription(recurrence.getDescription());
    transaction.setAmount(recurrence.getType().applySign(recurrence.getAmount()));
    transaction.setAccount(recurrence.getAccount());
    transaction.setCategory(recurrence.getCategory());
    transaction.setSubcategory(recurrence.getSubcategory());
    transaction.setExcludeFromTotals(recurrence.getExcludeFromTotals());
    return transaction;
  }
}
