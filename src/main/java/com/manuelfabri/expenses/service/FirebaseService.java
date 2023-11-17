package com.manuelfabri.expenses.service;

import com.google.firebase.auth.UserRecord;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.model.FirebaseTokenHolder;
import com.manuelfabri.expenses.model.Role;

public interface FirebaseService {

  public FirebaseLoginResponseDto login(String email, String password);

  public FirebaseTokenHolder parseToken(String token);

  public UserRecord getUserById(String uid);

  public void sendVerificationEmail(String token);

  public UserRecord createUserFromRequest(UserRegisterDto userRegisterData, Role startingRole);

}
