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
  @NotBlank(message = "Description cannot be blank or null")
  private String description;
  @NotNull
  private BigDecimal amount;
  @NotNull
  private Long accountId;
  @NotNull
  private Long categoryId;
  private Long subcategoryId;
}
