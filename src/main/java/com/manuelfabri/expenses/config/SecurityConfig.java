package com.manuelfabri.expenses.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.filter.FirebaseAuthorizationFilter;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private FirebaseService firebaseService;
  private UserService userService;

  @Value("${app.cors.allowed-origins:http://localhost:3000}")
  private List<String> allowedOrigins;

  public SecurityConfig(FirebaseService firebaseService, UserService userService) {
    this.firebaseService = firebaseService;
    this.userService = userService;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    if (allowedOrigins.contains("*")) {
      throw new IllegalStateException(
          "app.cors.allowed-origins cannot contain '*' because allowCredentials is enabled; "
              + "list explicit origins instead.");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Refresh-Token"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Actuator gets its own chain, evaluated before {@link #filterChain}, so infra health checks
   * (Railway, load balancers) never need a Firebase token. Only health/info are exposed at all (see
   * management.endpoints.web.exposure.include); any other actuator endpoint added later still falls
   * back to requiring an authenticated user, same as the rest of the app.
   */
  @Bean
  @Order(1)
  SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint()).csrf(csrf -> csrf.disable())
        .authorizeHttpRequests((authz) -> authz.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
            .permitAll().anyRequest().authenticated());
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    String authRouteMatcher = Urls.AUTH + "/**";
    String apiDocsRouteMatcher = Urls.API_DOCS + "/**";
    String swaggerUiRouteMatcher = Urls.SWAGGER_UI + "/**";

    return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests((authz) -> authz.requestMatchers(authRouteMatcher, apiDocsRouteMatcher, swaggerUiRouteMatcher)
            .permitAll().anyRequest().authenticated())
        .addFilterBefore(new FirebaseAuthorizationFilter(userService, firebaseService),
            UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
  }
}
