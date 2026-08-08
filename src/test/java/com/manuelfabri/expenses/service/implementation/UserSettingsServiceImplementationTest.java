package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.dto.UserSettingsRequestDto;
import com.manuelfabri.expenses.model.ThemeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.model.UserSettings;
import com.manuelfabri.expenses.repository.UserSettingsRepository;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceImplementationTest {

  @Mock
  private UserSettingsRepository userSettingsRepository;

  private UserSettingsServiceImplementation service;
  private User currentUser;

  @BeforeEach
  void setUp() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setFieldMatchingEnabled(true).setFieldAccessLevel(AccessLevel.PRIVATE)
        .setSkipNullEnabled(true);

    service = new UserSettingsServiceImplementation(userSettingsRepository, mapper);

    currentUser = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getSettingsReturnsDefaultsWhenNoRowExistsForTheCurrentUser() {
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());

    UserSettingsDto result = service.getSettings();

    assertThat(result.getTheme()).isEqualTo(ThemeEnum.SYSTEM);
    assertThat(result.getLocale()).isEqualTo("en");
  }

  @Test
  void getSettingsReturnsThePersistedRowForTheCurrentUser() {
    UserSettings existing = new UserSettings("owner-1");
    existing.setTheme(ThemeEnum.DARK);
    existing.setLocale("es");
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.of(existing));

    UserSettingsDto result = service.getSettings();

    assertThat(result.getTheme()).isEqualTo(ThemeEnum.DARK);
    assertThat(result.getLocale()).isEqualTo("es");
  }

  @Test
  void updateSettingsCreatesARowWhenNoneExistsYet() {
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());
    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserSettingsRequestDto request = new UserSettingsRequestDto();
    request.setTheme(ThemeEnum.DARK);
    request.setLocale("es-AR");

    UserSettingsDto result = service.updateSettings(request);

    ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
    verify(userSettingsRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo("owner-1");
    assertThat(result.getTheme()).isEqualTo(ThemeEnum.DARK);
    assertThat(result.getLocale()).isEqualTo("es-AR");
  }

  @Test
  void updateSettingsOnlyChangesFieldsThatArePresentOnTheRequest() {
    UserSettings existing = new UserSettings("owner-1");
    existing.setTheme(ThemeEnum.LIGHT);
    existing.setLocale("en");
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.of(existing));
    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserSettingsRequestDto request = new UserSettingsRequestDto();
    request.setTheme(ThemeEnum.DARK);

    UserSettingsDto result = service.updateSettings(request);

    assertThat(result.getTheme()).isEqualTo(ThemeEnum.DARK);
    assertThat(result.getLocale()).isEqualTo("en");
  }

}
