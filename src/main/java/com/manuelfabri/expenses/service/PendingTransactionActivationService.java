package com.manuelfabri.expenses.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.repository.TransactionRepository;

@Service
public class PendingTransactionActivationService {
  private TransactionRepository transactionRepository;

  public PendingTransactionActivationService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  // TODO: add a distributed lock (e.g. ShedLock) if this app is ever deployed across more than one instance.
  @Scheduled(cron = "0 10 0 * * *")
  public void activateDueTransactions() {
    transactionRepository.activateDuePendingTransactions(OffsetDateTime.now(ZoneOffset.UTC));
  }
}
