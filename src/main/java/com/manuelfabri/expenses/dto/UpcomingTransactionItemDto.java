package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.SourceTypeEnum;
import lombok.Data;

@Data
public class UpcomingTransactionItemDto {
  private SourceTypeEnum sourceType;
  private Long sourceId;
  private OffsetDateTime date;
  private String description;
  private String categoryIconName;
  private String categoryColor;
  private Long accountId;
  private BigDecimal signedAmount;
  private RecurrenceFrequencyEnum frequency;
  private Integer intervalDays;
  private Set<Integer> daysOfMonth;
}
