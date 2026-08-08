package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class UpcomingTransactionGroupDto {
  private String currency;
  private BigDecimal total;
  private List<UpcomingTransactionItemDto> items;
}
