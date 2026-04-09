package com.devikapps.caverne.modules.user;

import com.devikapps.caverne.modules.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthBearerFilter extends OncePerRequestFilter {

  private final AuthSessionResolver authSessionResolver;
  private final ObjectMapper objectMapper;

  public AuthBearerFilter(AuthSessionResolver authSessionResolver, ObjectMapper objectMapper) {
    this.authSessionResolver = authSessionResolver;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      UserAccount user = authSessionResolver.resolveUserOrNull(authorizationHeader);
      if (user != null) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
      filterChain.doFilter(request, response);
    } catch (ResponseStatusException exception) {
      SecurityContextHolder.clearContext();
      response.setStatus(exception.getStatusCode().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getWriter(),
          new ApiError(
              exception.getStatusCode().value(),
              exception.getReason() == null
                  ? HttpStatus.valueOf(exception.getStatusCode().value()).getReasonPhrase()
                  : exception.getReason()));
    }
  }
}
