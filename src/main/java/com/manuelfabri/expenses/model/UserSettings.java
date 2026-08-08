package com.manuelfabri.expenses.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity(name = "usersettings")
public class UserSettings {

  @Id
  @Column(name = "userid")
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ThemeEnum theme = ThemeEnum.SYSTEM;

  @Column(nullable = false, length = 10)
  private String locale = "en";

  public UserSettings() {}

  public UserSettings(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ThemeEnum getTheme() {
    return theme;
  }

  public void setTheme(ThemeEnum theme) {
    this.theme = theme;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

}
