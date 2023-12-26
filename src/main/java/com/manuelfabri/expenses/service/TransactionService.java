package com.manuelfabri.expenses.service;

import java.time.OffsetDateTime;
import java.util.List;
import com.manuelfabri.expenses.dto.CreateTransactionDto;
import com.manuelfabri.expenses.dto.TransactionDto;


public interface TransactionService {
  List<TransactionDto> getAllTransactions();

  TransactionDto createTransaction(CreateTransactionDto transactionDto);

  void deleteTransaction(Long id);

  TransactionDto getById(Long id);

  List<TransactionDto> getTransactionsByAccountId(Long id);

  List<TransactionDto> getTransactionsByCategoryId(Long id);

  List<TransactionDto> getTransactionsBySubcategoryId(Long id);

  List<TransactionDto> getMonthlyTransactions(int year, int month);
}
