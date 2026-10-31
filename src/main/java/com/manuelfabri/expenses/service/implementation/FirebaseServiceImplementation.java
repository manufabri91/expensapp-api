package com.manuelfabri.expenses.service.implementation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.manuelfabri.expenses.dto.FirebaseLoginResponseDto;
import com.manuelfabri.expenses.dto.FirebaseRefreshTokenResponseDto;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.DependencyException;
import com.manuelfabri.expenses.exception.DuplicateEmailException;
import com.manuelfabri.expenses.exception.InvalidLoginException;
import com.manuelfabri.expenses.model.FirebaseTokenHolder;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.service.FirebaseService;

@Service
public class FirebaseServiceImplementation implements FirebaseService {
  private static final Logger log = LoggerFactory.getLogger(FirebaseServiceImplementation.class);
  private final RestTemplate restTemplate;
  private final String apiKey;
  private final FirebaseAuth firebaseAuth;

  public FirebaseServiceImplementation(@Value("${firebase.api-key}") String apiKey,
      RestTemplateBuilder restTemplateBuilder, FirebaseAuth firebaseAuth) {
    this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(2))
        .setReadTimeout(Duration.ofSeconds(3)).build();
    this.apiKey = apiKey;
    this.firebaseAuth = firebaseAuth;
  }

  @Override
  public FirebaseTokenHolder parseToken(String token) {
    if (!StringUtils.hasText(token)) {
      throw new IllegalArgumentException("FirebaseTokenBlank");
    }
    try {
      FirebaseToken authTask = firebaseAuth.verifyIdToken(token);
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
    UserRecord user;
    try {
      user = firebaseAuth.createUser(createRequest);
    } catch (FirebaseAuthException firebaseException) {
      if (firebaseException.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
        throw new DuplicateEmailException();
      }
      throw dependencyFailure("create", firebaseException);
    }
    try {
      var udpateRequest = new UserRecord.UpdateRequest(user.getUid());
      Map<String, Object> claims = new HashMap<>();
      claims.put("roles", Collections.singletonList(startingRole.getSpringName()));
      udpateRequest.setCustomClaims(claims);
      user = firebaseAuth.updateUser(udpateRequest);
      return user;
    } catch (FirebaseAuthException | RuntimeException firebaseException) {
      try {
        firebaseAuth.deleteUser(user.getUid());
      } catch (FirebaseAuthException | RuntimeException cleanupException) {
        log.error("Registration stage=claims-cleanup requestId={} uid={} code={}",
            MDC.get("requestId"), user.getUid(), safeFailureCode(cleanupException));
      }
      throw dependencyFailure("claims", firebaseException);
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

      if (response.getStatusCode() != HttpStatus.OK) {
        throw new InvalidLoginException();
      }

      if (response.getBody() == null) {
        throw new DependencyException("Firebase API");
      }
      return response.getBody();

    } catch (HttpClientErrorException exception) {
      throw new InvalidLoginException();
    } catch (RestClientException exception) {
      log.warn("Firebase stage=login requestId={} cause={}", MDC.get("requestId"),
          exception.getClass().getSimpleName());
      throw new DependencyException("Firebase API");
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
      return firebaseAuth.getUser(uid);
    } catch (FirebaseAuthException e) {
      throw new InvalidLoginException();
    }
  }

  @Override
  public void deleteUser(String uid) {
    try {
      firebaseAuth.deleteUser(uid);
    } catch (FirebaseAuthException firebaseException) {
      throw dependencyFailure("delete", firebaseException);
    }
  }

  @Override
  public FirebaseRefreshTokenResponseDto refreshToken(String refreshToken) {
    try {
      String url = "https://securetoken.googleapis.com/v1/token?key=" + apiKey;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      // Create the form data (body)
      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("grant_type", "refresh_token");
      formData.add("refresh_token", refreshToken);

      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
      ResponseEntity<FirebaseRefreshTokenResponseDto> response =
          this.restTemplate.postForEntity(url, entity, FirebaseRefreshTokenResponseDto.class);

      if (response.getStatusCode() != HttpStatus.OK) {
        throw new InvalidLoginException();
      }

      return response.getBody();

    } catch (HttpClientErrorException exception) {
      throw new InvalidLoginException();
    } catch (RestClientException exception) {
      log.warn("Firebase stage=refresh requestId={} cause={}", MDC.get("requestId"),
          exception.getClass().getSimpleName());
      throw new DependencyException("Firebase API");
    }
  }

  private DependencyException dependencyFailure(String stage, Exception exception) {
    log.warn("Firebase stage={} requestId={} code={}", stage, MDC.get("requestId"), safeFailureCode(exception));
    return new DependencyException("Firebase");
  }

  private Object safeFailureCode(Exception exception) {
    return exception instanceof FirebaseAuthException firebaseException
        ? firebaseException.getAuthErrorCode() : exception.getClass().getSimpleName();
  }
}
