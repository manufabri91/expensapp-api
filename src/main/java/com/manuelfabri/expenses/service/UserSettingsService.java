package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.dto.UserSettingsRequestDto;

public interface UserSettingsService {

  UserSettingsDto getSettings();

  UserSettingsDto updateSettings(UserSettingsRequestDto userSettingsDto);

}
