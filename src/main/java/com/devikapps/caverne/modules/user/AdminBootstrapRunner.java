package com.devikapps.caverne.modules.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

  private final AdminBootstrapProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      return;
    }

    validateConfiguration();

    if (userRepository.existsByRole(UserRole.ADMIN)) {
      return;
    }

    userRepository.save(
        UserAccount.builder()
            .firstname(properties.getFirstname().trim())
            .lastname(properties.getLastname().trim())
            .email(normalize(properties.getEmail()))
            .phone(normalize(properties.getPhone()))
            .passwordHash(passwordEncoder.encode(properties.getPassword()))
            .authProvider(AuthProviderCode.LOCAL)
            .role(UserRole.ADMIN)
            .status("active")
            .build());
  }

  private void validateConfiguration() {
    if (isBlank(properties.getFirstname()) || isBlank(properties.getLastname())) {
      throw new IllegalStateException(
          "bootstrap.admin.firstname and bootstrap.admin.lastname are required when bootstrap.admin.enabled=true");
    }
    if (isBlank(properties.getPassword())) {
      throw new IllegalStateException(
          "bootstrap.admin.password is required when bootstrap.admin.enabled=true");
    }
    if (isBlank(properties.getEmail()) && isBlank(properties.getPhone())) {
      throw new IllegalStateException(
          "bootstrap.admin.email or bootstrap.admin.phone is required when bootstrap.admin.enabled=true");
    }
  }

  private String normalize(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
