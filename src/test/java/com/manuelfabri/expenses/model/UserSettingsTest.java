package com.manuelfabri.expenses.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserSettingsTest {

  @Test
  void noArgConstructor_defaultsToSystemThemeAndEnglishLocale() {
    UserSettings settings = new UserSettings();

    assertThat(settings.getUserId()).isNull();
    assertThat(settings.getTheme()).isEqualTo(ThemeEnum.SYSTEM);
    assertThat(settings.getLocale()).isEqualTo("en");
  }

  @Test
  void setUserId_updatesTheUserId() {
    UserSettings settings = new UserSettings();

    settings.setUserId("owner-1");

    assertThat(settings.getUserId()).isEqualTo("owner-1");
  }

}
