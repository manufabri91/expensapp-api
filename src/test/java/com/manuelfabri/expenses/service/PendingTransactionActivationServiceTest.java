package com.manuelfabri.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manuelfabri.expenses.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class PendingTransactionActivationServiceTest {

  @Mock
  private TransactionRepository transactionRepository;

  private PendingTransactionActivationService activationService;

  @BeforeEach
  void setUp() {
    activationService = new PendingTransactionActivationService(transactionRepository);
  }

  @Test
  void activateDueTransactions_callsRepositoryMethodWithCurrentTime() {
    OffsetDateTime beforeCall = OffsetDateTime.now(ZoneOffset.UTC);

    activationService.activateDueTransactions();

    OffsetDateTime afterCall = OffsetDateTime.now(ZoneOffset.UTC);

    ArgumentCaptor<OffsetDateTime> timeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(transactionRepository).activateDuePendingTransactions(timeCaptor.capture());

    OffsetDateTime capturedTime = timeCaptor.getValue();
    assertThat(capturedTime).isAfterOrEqualTo(beforeCall).isBeforeOrEqualTo(afterCall);
  }
}
