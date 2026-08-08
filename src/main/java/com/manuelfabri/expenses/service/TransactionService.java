package com.manuelfabri.expenses.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;


public interface TransactionService {
  TransactionDto createTransaction(TransactionRequestDto transactionDto);

  TransactionDto updateTransaction(Long id, TransactionRequestDto transactionDto);

  void deleteTransaction(Long id);

  TransactionDto confirm(Long id);

  List<TransactionDto> getPendingTransactions();

  Page<TransactionDto> getPagedTransactions(TransactionTypeEnum type, List<Long> categoryIds,
      List<Long> subcategoryIds, List<Long> accountIds, BigDecimal minAmount, BigDecimal maxAmount,
      OffsetDateTime fromDate, OffsetDateTime toDate, Pageable pageable);

  List<BalanceSummaryDto> getFilteredTotals(TransactionTypeEnum type, List<Long> categoryIds,
      List<Long> subcategoryIds, List<Long> accountIds, BigDecimal minAmount, BigDecimal maxAmount,
      OffsetDateTime fromDate, OffsetDateTime toDate);

  TransactionDto getById(Long id);

  List<TransactionDto> getTransactionsByAccountId(Long id);

  List<TransactionDto> getTransactionsByCategoryId(Long id);

  List<TransactionDto> getTransactionsBySubcategoryId(Long id);

  List<TransactionDto> getMonthlyTransactions(int year, int month);

}
