package com.manuelfabri.expenses.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.manuelfabri.expenses.exception.InvalidLoginException;
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

  public FirebaseAuthorizationFilter(UserService userService, FirebaseService firebaseService) {
    this.userService = userService;
    this.firebaseService = firebaseService;
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    if (request.getHeader(HEADER_NAME) == null) {
      filterChain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = request;
    String xAuth = httpRequest.getHeader(HEADER_NAME);
    try {
      FirebaseTokenHolder holder = firebaseService.parseToken(xAuth);
      User user = userService.getUserById(holder.getUid());
      Authentication auth = new UsernamePasswordAuthenticationToken(user, holder, holder.getRoles());
      SecurityContextHolder.getContext().setAuthentication(auth);
      filterChain.doFilter(request, response);
    } catch (InvalidLoginException e) {
      SecurityContextHolder.clearContext();
      response.setStatus(401);
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      throw new ServletException(e.getMessage());
    }

  }

}
