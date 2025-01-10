package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransactionRequestDto {
  private Long id;
  private OffsetDateTime eventDate = OffsetDateTime.now();
  @NotBlank(message = "MISSING_DESCRIPTION")
  private String description;
  @NotNull(message = "MISSING_AMOUNT")
  private BigDecimal amount;
  @NotNull(message = "MISSING_ACCOUNT")
  private Long accountId;
  @NotNull(message = "MISSING_CATEGORY")
  private Long categoryId;
  @NotNull(message = "MISSING_SUBCATEGORY")
  private Long subcategoryId;
}
