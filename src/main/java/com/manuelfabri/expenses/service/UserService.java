package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.model.User;

public interface UserService {
  User createUserFromRequest(UserRegisterDto userRegisterData, String userId, Role startingRole);

  User getUserById(String id);

}
