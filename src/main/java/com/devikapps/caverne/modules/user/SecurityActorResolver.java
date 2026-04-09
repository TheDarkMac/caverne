package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityActorResolver {

  public UserAccount requireUser() {
    UserAccount user = resolveUserOrNull();
    if (user == null) {
      throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
    }
    return user;
  }

  public UserAccount requireAdmin() {
    UserAccount user = requireUser();
    if (user.getRole() != UserRole.ADMIN) {
      throw new ResponseStatusException(FORBIDDEN, "Admin access required");
    }
    return user;
  }

  public UserAccount resolveUserOrNull() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    return principal instanceof UserAccount user ? user : null;
  }
}
