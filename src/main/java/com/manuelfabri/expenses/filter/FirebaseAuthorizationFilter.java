package com.manuelfabri.expenses.filter;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.ErrorResponseDto;
import com.manuelfabri.expenses.exception.InvalidLoginException;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.FirebaseTokenHolder;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class FirebaseAuthorizationFilter extends OncePerRequestFilter {
  private static final String HEADER_NAME = "Authorization";

  private FirebaseService firebaseService;
  private UserService userService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public FirebaseAuthorizationFilter(UserService userService, FirebaseService firebaseService) {
    this.userService = userService;
    this.firebaseService = firebaseService;
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    String xAuth = request.getHeader(HEADER_NAME);
    String requestURI = request.getRequestURI();

    // Bypass /auth URLs
    if (requestURI.startsWith(Urls.AUTH)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (xAuth == null || xAuth.isBlank()) {
      SecurityContextHolder.clearContext();
      writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
      return;
    }

    try {
      FirebaseTokenHolder holder = firebaseService.parseToken(xAuth);
      User user = userService.getUserById(holder.getUid());
      Authentication auth = new UsernamePasswordAuthenticationToken(user, holder, holder.getRoles());
      SecurityContextHolder.getContext().setAuthentication(auth);
      filterChain.doFilter(request, response);
    } catch (InvalidLoginException e) {
      SecurityContextHolder.clearContext();
      writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    } catch (ResourceNotFoundException e) {
      SecurityContextHolder.clearContext();
      writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return;
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      throw new ServletException(e.getMessage());
    }

  }

  /**
   * Writes a JSON error body matching the shape produced by GlobalExceptionHandler
   * ({@link ErrorResponseDto}), then commits the response. Callers must not invoke
   * filterChain.doFilter(...) afterwards.
   */
  private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), new ErrorResponseDto(message));
  }

}
