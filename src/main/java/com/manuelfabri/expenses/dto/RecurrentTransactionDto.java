package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import lombok.Data;

@Data
public class RecurrentTransactionDto {
  private long id;
  private TransactionTypeEnum type;
  private BigDecimal amount;
  private String description;
  private Long accountId;
  private String accountName;
  private CategoryDto category;
  private SubcategoryDto subcategory;
  private RecurrenceFrequencyEnum frequency;
  private Integer intervalDays;
  private Set<Integer> daysOfMonth;
  private OffsetDateTime startDate;
  private OffsetDateTime endDate;
  private RecurrenceStatusEnum status;
  private OffsetDateTime lastGeneratedDate;
  private OffsetDateTime nextDueDate;
  private boolean excludeFromTotals;
}
