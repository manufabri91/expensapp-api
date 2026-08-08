package com.manuelfabri.expenses.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.manuelfabri.expenses.model.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {
}
