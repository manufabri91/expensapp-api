package com.manuelfabri.expenses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.dto.UserSettingsRequestDto;
import com.manuelfabri.expenses.model.ThemeEnum;
import com.manuelfabri.expenses.service.UserSettingsService;

@WebMvcTest(controllers = UserSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserSettingsControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private UserSettingsService userSettingsService;

  private UserSettingsDto sampleDto(ThemeEnum theme, String locale) {
    UserSettingsDto dto = new UserSettingsDto();
    dto.setTheme(theme);
    dto.setLocale(locale);
    return dto;
  }

  @Test
  void getSettings_returnsOkWithTheServiceResult() throws Exception {
    when(userSettingsService.getSettings()).thenReturn(sampleDto(ThemeEnum.SYSTEM, "en"));

    mockMvc.perform(get("/user-settings")).andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("SYSTEM"))
        .andExpect(jsonPath("$.locale").value("en"));
  }

  @Test
  void updateSettings_returnsOkWithTheUpdatedSettings() throws Exception {
    when(userSettingsService.updateSettings(any())).thenReturn(sampleDto(ThemeEnum.DARK, "es-AR"));

    UserSettingsRequestDto requestDto = new UserSettingsRequestDto();
    requestDto.setTheme(ThemeEnum.DARK);
    requestDto.setLocale("es-AR");

    mockMvc
        .perform(patch("/user-settings").contentType("application/json")
            .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("DARK"))
        .andExpect(jsonPath("$.locale").value("es-AR"));
  }

}
