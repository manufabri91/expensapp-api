package com.manuelfabri.expenses.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.BalanceSummaryDto;
import com.manuelfabri.expenses.dto.MonthlyBalanceSummaryDto;
import com.manuelfabri.expenses.dto.ProgrammedTransactionsDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionGroupDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionItemDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionsResponseDto;
import com.manuelfabri.expenses.model.SourceTypeEnum;
import com.manuelfabri.expenses.service.SummaryService;
import com.manuelfabri.expenses.service.TransactionStatisticsService;
import com.manuelfabri.expenses.service.UpcomingTransactionService;

@WebMvcTest(controllers = SummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class SummaryControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private SummaryService summaryService;
  @MockBean
  private TransactionStatisticsService transactionStatisticsService;
  @MockBean
  private UpcomingTransactionService upcomingTransactionService;

  @Test
  void getDefaultSummary_returnsOkWithTheServiceList() throws Exception {
    BalanceSummaryDto dto = new BalanceSummaryDto();
    when(summaryService.getMonthlySummaryWithTotalBalance(2024, 1)).thenReturn(List.of(dto));

    mockMvc.perform(get("/summary").param("year", "2024").param("month", "1")).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void getUpcomingTransactions_returnsOkWithTheServiceResponse() throws Exception {
    UpcomingTransactionItemDto item = new UpcomingTransactionItemDto();
    item.setSourceType(SourceTypeEnum.RECURRING);
    item.setSourceId(1L);
    item.setSignedAmount(new BigDecimal("-30.00"));

    UpcomingTransactionGroupDto group = new UpcomingTransactionGroupDto();
    group.setCurrency("USD");
    group.setTotal(new BigDecimal("-30.00"));
    group.setItems(List.of(item));

    UpcomingTransactionsResponseDto response = new UpcomingTransactionsResponseDto();
    response.setExpenses(List.of(group));
    response.setIncomes(List.of());
    when(upcomingTransactionService.getUpcomingTransactions()).thenReturn(response);

    mockMvc.perform(get("/summary/upcoming-transactions")).andExpect(status().isOk())
        .andExpect(jsonPath("$.expenses.length()").value(1))
        .andExpect(jsonPath("$.expenses[0].currency").value("USD"))
        .andExpect(jsonPath("$.expenses[0].total").value(-30.00))
        .andExpect(jsonPath("$.expenses[0].items[0].sourceId").value(1))
        .andExpect(jsonPath("$.incomes.length()").value(0));
  }

  @Test
  void getProgrammedTransactions_returnsOkWithTheServiceResponse() throws Exception {
    UpcomingTransactionItemDto expenseItem = new UpcomingTransactionItemDto();
    expenseItem.setSourceType(SourceTypeEnum.ONE_TIME);
    expenseItem.setSourceId(5L);
    expenseItem.setSignedAmount(new BigDecimal("-12.50"));

    ProgrammedTransactionsDto response = new ProgrammedTransactionsDto();
    response.setExpenses(List.of(expenseItem));
    response.setIncomes(List.of());
    when(upcomingTransactionService.getProgrammedTransactions()).thenReturn(response);

    mockMvc.perform(get("/summary/programmed-transactions")).andExpect(status().isOk())
        .andExpect(jsonPath("$.expenses.length()").value(1))
        .andExpect(jsonPath("$.expenses[0].sourceId").value(5))
        .andExpect(jsonPath("$.expenses[0].sourceType").value("ONE_TIME"))
        .andExpect(jsonPath("$.incomes.length()").value(0));
  }

  @Test
  void getMonthlyHistory_withIncludePendingTrue_passesTheFlagThroughToTheService() throws Exception {
    MonthlyBalanceSummaryDto dto = new MonthlyBalanceSummaryDto();
    when(transactionStatisticsService.getMonthlyHistory(6, true)).thenReturn(List.of(dto));

    mockMvc.perform(get("/summary/monthly-history/6").param("includePending", "true")).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    verify(transactionStatisticsService).getMonthlyHistory(6, true);
  }

  @Test
  void getMonthlyHistory_withoutIncludePendingParam_defaultsToFalse() throws Exception {
    MonthlyBalanceSummaryDto dto = new MonthlyBalanceSummaryDto();
    when(transactionStatisticsService.getMonthlyHistory(6, false)).thenReturn(List.of(dto));

    mockMvc.perform(get("/summary/monthly-history/6")).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    verify(transactionStatisticsService).getMonthlyHistory(6, false);
  }
}
