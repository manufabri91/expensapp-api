package com.manuelfabri.expenses.dto;

import com.manuelfabri.expenses.model.ThemeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSettingsDto {

  @NotNull
  private ThemeEnum theme;
  @NotNull
  private String locale;
}
