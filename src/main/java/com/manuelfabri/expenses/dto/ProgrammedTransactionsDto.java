package com.manuelfabri.expenses.dto;

import java.util.List;
import lombok.Data;

@Data
public class ProgrammedTransactionsDto {
  private List<UpcomingTransactionItemDto> expenses;
  private List<UpcomingTransactionItemDto> incomes;
}
