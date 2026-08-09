package com.manuelfabri.expenses.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.UserDataExportDto;
import com.manuelfabri.expenses.service.PrivacyService;

@RestController
@RequestMapping(Urls.PRIVACY)
public class PrivacyController {

  private PrivacyService privacyService;

  public PrivacyController(PrivacyService privacyService) {
    this.privacyService = privacyService;
  }

  @GetMapping("/export")
  public ResponseEntity<UserDataExportDto> exportUserData() {
    return new ResponseEntity<>(privacyService.exportUserData(), HttpStatus.OK);
  }

  @DeleteMapping("/account")
  public ResponseEntity<Void> deleteAccount() {
    privacyService.deleteCurrentUserAccount();
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
