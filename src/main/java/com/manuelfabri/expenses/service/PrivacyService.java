package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.UserDataExportDto;

public interface PrivacyService {
  UserDataExportDto exportUserData();

  void deleteCurrentUserAccount();
}
