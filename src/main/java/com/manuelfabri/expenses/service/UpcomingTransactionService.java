package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.ProgrammedTransactionsDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionsResponseDto;

public interface UpcomingTransactionService {
  UpcomingTransactionsResponseDto getUpcomingTransactions();

  ProgrammedTransactionsDto getProgrammedTransactions();
}
