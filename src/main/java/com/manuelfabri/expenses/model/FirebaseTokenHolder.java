package com.manuelfabri.expenses.model;

import com.google.firebase.auth.FirebaseToken;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseTokenHolder {

  private FirebaseToken token;

  public FirebaseTokenHolder(FirebaseToken token) {
    this.token = token;
  }

  public String getEmail() {
    return token.getEmail();
  }

  public String getIssuer() {
    return token.getIssuer();
  }

  public String getName() {
    return token.getName();
  }

  public String getUid() {
    return token.getUid();
  }

  public boolean isEmailVerified() {
    return token.isEmailVerified();
  }

  public Map<String, Object> getClaims() {
    return token.getClaims();
  }

  public List<GrantedAuthority> getRoles() {
    List<GrantedAuthority> roles = new ArrayList<>();
    Set<String> keys = token.getClaims().keySet();
    keys.forEach(role -> {
      roles.add(new SimpleGrantedAuthority(role));
    });
    return roles;
  }
}
