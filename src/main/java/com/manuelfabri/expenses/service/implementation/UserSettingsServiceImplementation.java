package com.manuelfabri.expenses.service.implementation;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.dto.UserSettingsRequestDto;
import com.manuelfabri.expenses.model.ThemeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.model.UserSettings;
import com.manuelfabri.expenses.repository.UserSettingsRepository;
import com.manuelfabri.expenses.service.UserSettingsService;

@Service
public class UserSettingsServiceImplementation implements UserSettingsService {

  private UserSettingsRepository userSettingsRepository;
  private ModelMapper mapper;

  public UserSettingsServiceImplementation(UserSettingsRepository userSettingsRepository, ModelMapper mapper) {
    this.userSettingsRepository = userSettingsRepository;
    this.mapper = mapper;
  }

  private String getCurrentUserId() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getId();
  }

  @Override
  public UserSettingsDto getSettings() {
    UserSettings settings = this.userSettingsRepository.findById(getCurrentUserId())
        .orElseGet(() -> new UserSettings(getCurrentUserId()));

    return mapper.map(settings, UserSettingsDto.class);
  }

  @Override
  public UserSettingsDto updateSettings(UserSettingsRequestDto userSettingsDto) {
    String userId = getCurrentUserId();
    UserSettings settings = this.userSettingsRepository.findById(userId).orElseGet(() -> new UserSettings(userId));

    if (userSettingsDto.getTheme() != null) {
      settings.setTheme(userSettingsDto.getTheme());
    }
    if (userSettingsDto.getLocale() != null) {
      settings.setLocale(userSettingsDto.getLocale());
    }

    UserSettings updatedSettings = this.userSettingsRepository.save(settings);

    return mapper.map(updatedSettings, UserSettingsDto.class);
  }

}
