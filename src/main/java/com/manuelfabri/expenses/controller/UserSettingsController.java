package com.manuelfabri.expenses.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.dto.UserSettingsRequestDto;
import com.manuelfabri.expenses.service.UserSettingsService;

@RestController
@RequestMapping(Urls.USER_SETTINGS)
public class UserSettingsController {

  private UserSettingsService userSettingsService;

  public UserSettingsController(UserSettingsService userSettingsService) {
    this.userSettingsService = userSettingsService;
  }

  @GetMapping
  public ResponseEntity<UserSettingsDto> getSettings() {
    return new ResponseEntity<>(userSettingsService.getSettings(), HttpStatus.OK);
  }

  @PatchMapping
  public ResponseEntity<UserSettingsDto> updateSettings(@RequestBody UserSettingsRequestDto userSettingsDto) {
    return new ResponseEntity<>(userSettingsService.updateSettings(userSettingsDto), HttpStatus.OK);
  }

}
