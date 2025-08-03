package com.manuelfabri.expenses.dto;

import lombok.Data;

@Data
public class LinkedTransactionDTO {
  private Long id;
  private Long accountId;
  private String accountName;
}
