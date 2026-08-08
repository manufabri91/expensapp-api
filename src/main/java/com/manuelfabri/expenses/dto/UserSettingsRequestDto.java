package com.manuelfabri.expenses.dto;

import com.manuelfabri.expenses.model.ThemeEnum;
import lombok.Data;

@Data
public class UserSettingsRequestDto {

  private ThemeEnum theme;
  private String locale;
}
