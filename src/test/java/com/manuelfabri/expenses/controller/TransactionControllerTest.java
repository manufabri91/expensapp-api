package com.manuelfabri.expenses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.service.TransactionService;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private TransactionService transactionService;

  private TransactionDto sampleDto(Long id) {
    TransactionDto dto = new TransactionDto();
    dto.setId(id);
    dto.setEventDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    dto.setDescription("Weekly groceries");
    dto.setAmount(new BigDecimal("-45.50"));
    dto.setType(TransactionTypeEnum.EXPENSE);
    dto.setCurrencyCode(CurrencyEnum.USD);
    dto.setAccountId(10L);
    dto.setAccountName("Checking");

    CategoryDto category = new CategoryDto();
    category.setId(20L);
    category.setName("Groceries");
    dto.setCategory(category);

    SubcategoryDto subcategory = new SubcategoryDto();
    subcategory.setId(30L);
    subcategory.setName("Supermarket");
    dto.setSubcategory(subcategory);

    return dto;
  }

  @Test
  void confirm_delegatesToTheServiceAndReturnsOkWithTheMappedBody() throws Exception {
    TransactionDto confirmedDto = sampleDto(1L);
    confirmedDto.setExcludeFromTotals(false);
    confirmedDto.setPending(false);
    when(transactionService.confirm(1L)).thenReturn(confirmedDto);

    mockMvc.perform(patch("/transaction/{id}/confirm", 1L)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.pending").value(false))
        .andExpect(jsonPath("$.excludeFromTotals").value(false));
    verify(transactionService).confirm(1L);
  }

  @Test
  void confirm_returnsNotFound_whenServiceThrowsResourceNotFound() throws Exception {
    when(transactionService.confirm(404L)).thenThrow(new ResourceNotFoundException("Transaction", "id", "404"));

    mockMvc.perform(patch("/transaction/{id}/confirm", 404L)).andExpect(status().isNotFound());
  }

  @Test
  void getPendingTransactions_returnsOkWithTheServiceList() throws Exception {
    TransactionDto pendingDto = sampleDto(1L);
    pendingDto.setPending(true);
    pendingDto.setExcludeFromTotals(true);
    when(transactionService.getPendingTransactions()).thenReturn(List.of(pendingDto));

    mockMvc.perform(get("/transaction/pending")).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].pending").value(true));
  }

  @Test
  void getPendingTransactions_isNotSwallowedByTheIdOrYearMonthRoutes() throws Exception {
    // The controller also exposes GET /transaction/{id} (Long) and GET /transaction/{year}/{month} (int, int).
    // "pending" is a single path segment, same shape as {id}, so this proves Spring routes the literal
    // "/pending" segment to getPendingTransactions() rather than attempting (and failing) to parse it as a Long id.
    when(transactionService.getPendingTransactions()).thenReturn(List.of());

    mockMvc.perform(get("/transaction/pending")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());

    verify(transactionService).getPendingTransactions();
    verify(transactionService, never()).getById(any());
  }
}
