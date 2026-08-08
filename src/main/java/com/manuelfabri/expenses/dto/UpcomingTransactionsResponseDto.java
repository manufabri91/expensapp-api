package com.manuelfabri.expenses.dto;

import java.util.List;
import lombok.Data;

@Data
public class UpcomingTransactionsResponseDto {
  private List<UpcomingTransactionGroupDto> expenses;
  private List<UpcomingTransactionGroupDto> incomes;
}
