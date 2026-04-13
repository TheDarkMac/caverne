package com.devikapps.caverne.modules.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthBearerFilter extends OncePerRequestFilter {

  private final AuthSessionResolver authSessionResolver;

  public AuthBearerFilter(AuthSessionResolver authSessionResolver) {
    this.authSessionResolver = authSessionResolver;
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
      String rawToken = authSessionResolver.extractBearerToken(authorizationHeader);
      UserAccount user = authSessionResolver.resolveUserOrNull(authorizationHeader);
      if (user != null) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                rawToken,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
      filterChain.doFilter(request, response);
    } catch (ResponseStatusException exception) {
      SecurityContextHolder.clearContext();
      filterChain.doFilter(request, response);
    }
  }
}
