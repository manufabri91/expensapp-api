package com.manuelfabri.expenses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.service.RecurrentTransactionService;

@WebMvcTest(controllers = RecurrentTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecurrentTransactionControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private RecurrentTransactionService recurrentTransactionService;

  private RecurrentTransactionDto sampleDto(Long id) {
    RecurrentTransactionDto dto = new RecurrentTransactionDto();
    dto.setId(id);
    dto.setType(TransactionTypeEnum.EXPENSE);
    dto.setAmount(new BigDecimal("9.99"));
    dto.setDescription("Streaming subscription");
    dto.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    dto.setIntervalDays(30);
    dto.setStatus(RecurrenceStatusEnum.ACTIVE);
    dto.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    return dto;
  }

  private RecurrentTransactionRequestDto sampleRequestDto() {
    RecurrentTransactionRequestDto requestDto = new RecurrentTransactionRequestDto();
    requestDto.setType(TransactionTypeEnum.EXPENSE);
    requestDto.setAmount(new BigDecimal("9.99"));
    requestDto.setDescription("Streaming subscription");
    requestDto.setAccountId(1L);
    requestDto.setCategoryId(2L);
    requestDto.setSubcategoryId(3L);
    requestDto.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    requestDto.setIntervalDays(30);
    return requestDto;
  }

  @Test
  void getActiveForCurrentUser_returnsOkWithTheServiceList() throws Exception {
    when(recurrentTransactionService.getActiveForCurrentUser()).thenReturn(List.of(sampleDto(1L), sampleDto(2L)));

    mockMvc.perform(get("/recurrent-transaction")).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));
  }

  @Test
  void getById_returnsOkWithTheMatchingRecurrence() throws Exception {
    when(recurrentTransactionService.getById(1L)).thenReturn(sampleDto(1L));

    mockMvc.perform(get("/recurrent-transaction/{id}", 1L)).andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Streaming subscription"));
  }

  @Test
  void getById_returnsNotFound_whenServiceThrowsResourceNotFound() throws Exception {
    when(recurrentTransactionService.getById(404L))
        .thenThrow(new ResourceNotFoundException("RecurrentTransaction", "id", "404"));

    mockMvc.perform(get("/recurrent-transaction/{id}", 404L)).andExpect(status().isNotFound());
  }

  @Test
  void create_returnsCreatedWithTheSavedRecurrence() throws Exception {
    when(recurrentTransactionService.create(any())).thenReturn(sampleDto(1L));

    mockMvc
        .perform(post("/recurrent-transaction").contentType("application/json")
            .content(objectMapper.writeValueAsString(sampleRequestDto())))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void create_returnsBadRequest_whenServiceRejectsTheRequest() throws Exception {
    when(recurrentTransactionService.create(any())).thenThrow(new IllegalArgumentException("MISSING_INTERVAL_DAYS"));

    mockMvc
        .perform(post("/recurrent-transaction").contentType("application/json")
            .content(objectMapper.writeValueAsString(sampleRequestDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_returnsOkWithTheUpdatedRecurrence() throws Exception {
    when(recurrentTransactionService.update(eq(1L), any())).thenReturn(sampleDto(1L));

    mockMvc
        .perform(put("/recurrent-transaction/{id}", 1L).contentType("application/json")
            .content(objectMapper.writeValueAsString(sampleRequestDto())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void pause_delegatesToTheServiceAndReturnsOk() throws Exception {
    RecurrentTransactionDto pausedDto = sampleDto(1L);
    pausedDto.setStatus(RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionService.pause(1L)).thenReturn(pausedDto);

    mockMvc.perform(patch("/recurrent-transaction/{id}/pause", 1L)).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAUSED"));
    verify(recurrentTransactionService).pause(1L);
  }

  @Test
  void resume_delegatesToTheServiceAndReturnsOk() throws Exception {
    when(recurrentTransactionService.resume(1L)).thenReturn(sampleDto(1L));

    mockMvc.perform(patch("/recurrent-transaction/{id}/resume", 1L)).andExpect(status().isOk());
    verify(recurrentTransactionService).resume(1L);
  }

  @Test
  void cancel_delegatesToTheServiceAndReturnsOk() throws Exception {
    RecurrentTransactionDto cancelledDto = sampleDto(1L);
    cancelledDto.setStatus(RecurrenceStatusEnum.CANCELLED);
    when(recurrentTransactionService.cancel(1L)).thenReturn(cancelledDto);

    mockMvc.perform(patch("/recurrent-transaction/{id}/cancel", 1L)).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    verify(recurrentTransactionService).cancel(1L);
  }

  @Test
  void delete_delegatesToTheServiceAndReturnsOk() throws Exception {
    mockMvc.perform(delete("/recurrent-transaction/{id}", 1L)).andExpect(status().isOk());

    verify(recurrentTransactionService).delete(1L);
  }
}
