package com.manuelfabri.expenses.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.dto.TransactionDto;


public interface TransactionService {
  List<TransactionDto> getAllTransactions();

  TransactionDto createTransaction(TransactionRequestDto transactionDto);

  TransactionDto updateTransaction(Long id, TransactionRequestDto transactionDto);

  void deleteTransaction(Long id);

  Page<TransactionDto> getPagedTransactions(TransactionTypeEnum type, Long categoryId, BigDecimal minAmount,
      BigDecimal maxAmount, OffsetDateTime fromDate, OffsetDateTime toDate, Pageable pageable);

  TransactionDto getById(Long id);

  List<TransactionDto> getTransactionsByAccountId(Long id);

  List<TransactionDto> getTransactionsByCategoryId(Long id);

  List<TransactionDto> getTransactionsBySubcategoryId(Long id);

  List<TransactionDto> getMonthlyTransactions(int year, int month);

}
