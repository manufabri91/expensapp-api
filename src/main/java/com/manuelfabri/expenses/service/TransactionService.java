package com.manuelfabri.expenses.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.dto.TransactionDto;


public interface TransactionService {
  List<TransactionDto> getAllTransactions();

  Page<TransactionDto> getPagedTransactions(Pageable pageable);

  TransactionDto createTransaction(TransactionRequestDto transactionDto);

  void deleteTransaction(Long id);

  TransactionDto getById(Long id);

  List<TransactionDto> getTransactionsByAccountId(Long id);

  List<TransactionDto> getTransactionsByCategoryId(Long id);

  List<TransactionDto> getTransactionsBySubcategoryId(Long id);

  List<TransactionDto> getMonthlyTransactions(int year, int month);
}
