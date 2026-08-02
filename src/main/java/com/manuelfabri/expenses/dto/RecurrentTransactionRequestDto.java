package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecurrentTransactionRequestDto {
  private Long id;
  @NotNull(message = "MISSING_TYPE")
  private TransactionTypeEnum type;
  @NotNull(message = "MISSING_AMOUNT")
  private BigDecimal amount;
  @NotBlank(message = "MISSING_DESCRIPTION")
  private String description;
  @NotNull(message = "MISSING_ACCOUNT")
  private Long accountId;
  @NotNull(message = "MISSING_CATEGORY")
  private Long categoryId;
  @NotNull(message = "MISSING_SUBCATEGORY")
  private Long subcategoryId;
  @NotNull(message = "MISSING_FREQUENCY")
  private RecurrenceFrequencyEnum frequency;
  private Integer intervalDays;
  private Set<Integer> daysOfMonth = new HashSet<>();
  private OffsetDateTime startDate = OffsetDateTime.now();
  private OffsetDateTime endDate;
  private boolean excludeFromTotals = false;
}
