package com.devikapps.caverne.modules.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.caverne.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    properties = {
      "bootstrap.admin.enabled=true",
      "bootstrap.admin.firstname=Bootstrap",
      "bootstrap.admin.lastname=Owner",
      "bootstrap.admin.email=bootstrap-admin@example.com",
      "bootstrap.admin.password=bootstrap-secret-123"
    })
@Import(TestcontainersConfiguration.class)
class AdminBootstrapIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void shouldCreateInitialAdminWhenBootstrapIsEnabled() {
    UserAccount admin =
        userRepository.findByEmailIgnoreCase("bootstrap-admin@example.com").orElseThrow();

    assertEquals(UserRole.ADMIN, admin.getRole());
    assertEquals(AuthProviderCode.LOCAL, admin.getAuthProvider());
    assertEquals("Bootstrap", admin.getFirstname());
    assertEquals("Owner", admin.getLastname());
    assertEquals("active", admin.getStatus());
    assertTrue(passwordEncoder.matches("bootstrap-secret-123", admin.getPasswordHash()));
  }
}
