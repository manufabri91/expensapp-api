package com.manuelfabri.expenses.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.filter.FirebaseAuthorizationFilter;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private FirebaseService firebaseService;
  private UserService userService;

  public SecurityConfig(FirebaseService firebaseService, UserService userService) {
    this.firebaseService = firebaseService;
    this.userService = userService;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    String transactionRouteMatcher = Urls.TRANSACTION + "/**";
    String accountRouteMatcher = Urls.ACCOUNT + "/**";
    String categoryRouteMatcher = Urls.CATEGORY + "/**";
    String subcategoryRouteMatcher = Urls.SUBCATEGORY + "/**";
    String summaryRouteMatcher = Urls.SUMMARY + "/**";
    String authRouteMatcher = Urls.AUTH + "/**";

    return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.disable())
        .securityMatcher(transactionRouteMatcher, categoryRouteMatcher, subcategoryRouteMatcher, accountRouteMatcher,
            summaryRouteMatcher)
        .authorizeHttpRequests(
            (authz) -> authz.requestMatchers(authRouteMatcher).permitAll().anyRequest().authenticated())
        .addFilterBefore(new FirebaseAuthorizationFilter(userService, firebaseService),
            UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
  }
}
