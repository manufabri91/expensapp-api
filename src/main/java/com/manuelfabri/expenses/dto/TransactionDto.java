package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.TransactionTypeEnum;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransactionDto {

  @NotNull
  private long id;
  @NotNull
  private OffsetDateTime eventDate;
  @NotBlank(message = "Description cannot be blank or null")
  private String description;
  @Positive
  @Negative
  private BigDecimal amount;
  private TransactionTypeEnum type;
  @NotNull
  private CurrencyEnum currencyCode;
  @NotNull
  private Long accountId;
  @NotNull
  private String accountName;
  @NotNull
  private CategoryDto category;
  @NotNull
  private SubcategoryDto subcategory;
  private LinkedTransactionDTO linkedTransaction;
  private boolean excludeFromTotals = false;
}
