package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;

public interface SummaryService {
  List<BalanceSummaryDto> getSummary();

  List<BalanceSummaryDto> getSummary(int year);

  List<BalanceSummaryDto> getSummary(int year, int month);

  List<BalanceSummaryDto> getMonthlySummaryWithTotalBalance(int month, int year);
}
