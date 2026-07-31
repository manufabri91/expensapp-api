package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionRequestDto;

public interface RecurrentTransactionService {
  List<RecurrentTransactionDto> getActiveForCurrentUser();

  RecurrentTransactionDto getById(Long id);

  RecurrentTransactionDto create(RecurrentTransactionRequestDto requestDto);

  RecurrentTransactionDto update(Long id, RecurrentTransactionRequestDto requestDto);

  RecurrentTransactionDto pause(Long id);

  RecurrentTransactionDto resume(Long id);

  RecurrentTransactionDto cancel(Long id);

  void delete(Long id);
}
