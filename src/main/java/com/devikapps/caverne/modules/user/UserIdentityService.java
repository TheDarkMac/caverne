package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserIdentityService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserAccount resolveOrCreateExternalUser(
      AuthProviderCode provider,
      String externalAuthId,
      String email,
      String phone,
      String firstname,
      String lastname) {
    UserAccount existingByProvider =
        userRepository.findByAuthProviderAndExternalAuthId(provider, externalAuthId).orElse(null);
    if (existingByProvider != null) {
      applyIdentityUpdates(existingByProvider, email, phone, firstname, lastname);
      return userRepository.save(existingByProvider);
    }

    UserAccount existingByEmail =
        email == null ? null : userRepository.findByEmailIgnoreCase(email).orElse(null);
    UserAccount existingByPhone = phone == null ? null : userRepository.findByPhone(phone).orElse(null);

    if (existingByEmail != null
        && existingByPhone != null
        && !existingByEmail.getId().equals(existingByPhone.getId())) {
      throw new ResponseStatusException(
          UNAUTHORIZED, "External identity matches multiple local users");
    }

    UserAccount linkedUser = existingByEmail != null ? existingByEmail : existingByPhone;
    if (linkedUser != null) {
      if (linkedUser.getAuthProvider() != null
          && linkedUser.getExternalAuthId() != null
          && (linkedUser.getAuthProvider() != provider
              || !linkedUser.getExternalAuthId().equals(externalAuthId))) {
        throw new ResponseStatusException(
            UNAUTHORIZED, "External identity is already linked to another account");
      }
      linkedUser.setAuthProvider(provider);
      linkedUser.setExternalAuthId(externalAuthId);
      applyIdentityUpdates(linkedUser, email, phone, firstname, lastname);
      return userRepository.save(linkedUser);
    }

    UserAccount createdUser =
        UserAccount.builder()
            .firstname(defaultFirstname(firstname, email, phone))
            .lastname(defaultLastname(lastname))
            .email(email)
            .phone(phone)
            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
            .authProvider(provider)
            .externalAuthId(externalAuthId)
            .role(UserRole.SIMPLE_USER)
            .status("active")
            .build();
    return userRepository.save(createdUser);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> asMap(Object rawValue) {
    if (rawValue instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  private void applyIdentityUpdates(
      UserAccount user, String email, String phone, String firstname, String lastname) {
    if (email != null && !email.isBlank()) {
      user.setEmail(email);
    }
    if (phone != null && !phone.isBlank()) {
      user.setPhone(phone);
    }
    if (firstname != null && !firstname.isBlank()) {
      user.setFirstname(firstname);
    }
    if (lastname != null && !lastname.isBlank()) {
      user.setLastname(lastname);
    }
  }

  private String defaultFirstname(String firstname, String email, String phone) {
    if (firstname != null && !firstname.isBlank()) {
      return firstname;
    }
    if (email != null && !email.isBlank() && email.contains("@")) {
      return email.substring(0, email.indexOf('@'));
    }
    if (phone != null && !phone.isBlank()) {
      return phone;
    }
    return "User";
  }

  private String defaultLastname(String lastname) {
    if (lastname != null && !lastname.isBlank()) {
      return lastname;
    }
    return "Supabase";
  }
}
