package com.manuelfabri.expenses.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.UserDataExportDto;
import com.manuelfabri.expenses.service.PrivacyService;

@WebMvcTest(controllers = PrivacyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PrivacyControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private PrivacyService privacyService;

  @Test
  void exportUserData_returnsOkWithTheServiceResult() throws Exception {
    UserDataExportDto export = new UserDataExportDto();
    export.setEmail("owner@example.com");
    when(privacyService.exportUserData()).thenReturn(export);

    mockMvc.perform(get("/privacy/export")).andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("owner@example.com"));
  }

  @Test
  void deleteAccount_invokesTheServiceAndReturnsOk() throws Exception {
    mockMvc.perform(delete("/privacy/account")).andExpect(status().isOk());

    verify(privacyService).deleteCurrentUserAccount();
  }

}
