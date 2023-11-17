package com.manuelfabri.expenses.service.implementation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.DependencyException;
import com.manuelfabri.expenses.exception.InvalidLoginException;
import com.manuelfabri.expenses.model.FirebaseTokenHolder;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.service.FirebaseService;

@Service
public class FirebaseServiceImplementation implements FirebaseService {
  private final RestTemplate restTemplate;
  private final String apiKey;

  public FirebaseServiceImplementation(@Value("${firebase.api-key}") String apiKey,
      RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.apiKey = apiKey;
  }

  @Override
  public FirebaseTokenHolder parseToken(String token) {
    if (!StringUtils.hasText(token)) {
      throw new IllegalArgumentException("FirebaseTokenBlank");
    }
    try {
      FirebaseToken authTask = FirebaseAuth.getInstance().verifyIdToken(token);
      return new FirebaseTokenHolder(authTask);
    } catch (FirebaseAuthException firebaseException) {
      throw new InvalidLoginException();
    }
  }

  @Override
  public UserRecord createUserFromRequest(UserRegisterDto userRegisterData, Role startingRole) {
    String userDisplayName = String.format("%s %s", userRegisterData.getFirstName(), userRegisterData.getLastName());

    // Esto es necesario para poder usar las features de firebase de verificar email etce etc
    var createRequest = new UserRecord.CreateRequest();
    createRequest.setEmail(userRegisterData.getEmail());
    createRequest.setEmailVerified(false);
    createRequest.setDisplayName(userDisplayName);
    createRequest.setPassword(userRegisterData.getPassword());
    try {
      FirebaseAuth fbAuth = FirebaseAuth.getInstance();
      var user = fbAuth.createUser(createRequest);
      var udpateRequest = new UserRecord.UpdateRequest(user.getUid());
      Map<String, Object> claims = new HashMap<>();
      claims.put("roles", Collections.singletonList(startingRole.getSpringName()));
      udpateRequest.setCustomClaims(claims);
      user = fbAuth.updateUser(udpateRequest);
      return user;
    } catch (FirebaseAuthException firebaseException) {
      throw new DependencyException("Firebase", firebaseException.getMessage());
    }
  }

  @Override
  public FirebaseLoginResponseDto login(String email, String password) {
    // FURTHER INFO ON https://firebase.google.com/docs/reference/rest/auth
    try {
      String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;

      // set req. headers
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      // set reqbody
      Map<String, Object> body = new HashMap<>();
      body.put("email", email);
      body.put("password", password);
      body.put("returnSecureToken", true);

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
      ResponseEntity<FirebaseLoginResponseDto> response =
          this.restTemplate.postForEntity(url, entity, FirebaseLoginResponseDto.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        return response.getBody();
      } else {
        throw new InvalidLoginException();
      }
    } catch (RestClientException exception) {
      throw new DependencyException("Firebase API", exception.getMessage());
    }
  }

  @Override
  public void sendVerificationEmail(String token) {
    // FURTHER INFO ON https://firebase.google.com/docs/reference/rest/auth
    String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + apiKey;

    // set req. headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    // set reqbody
    Map<String, Object> body = new HashMap<>();
    body.put("requestType", "VERIFY_EMAIL");
    body.put("idToken", token);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
    this.restTemplate.postForEntity(url, entity, FirebaseLoginResponseDto.class);
  }

  @Override
  public UserRecord getUserById(String uid) {
    try {
      return FirebaseAuth.getInstance().getUser(uid);
    } catch (FirebaseAuthException e) {
      throw new InvalidLoginException();
    }
  }
}
