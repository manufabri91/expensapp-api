package com.manuelfabri.expenses.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserDataExportDto {
  private String id;
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private OffsetDateTime termsAcceptedAt;
  private String termsVersion;
  private UserSettingsDto settings;
  private List<AccountDto> accounts;
  private List<TransactionDto> transactions;
  private List<CategoryDto> categories;
  private List<SubcategoryDto> subcategories;
  private List<RecurrentTransactionDto> recurrentTransactions;
  private OffsetDateTime exportedAt;
}
