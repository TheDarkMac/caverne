package com.devikapps.caverne.modules.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
    properties = {
      "auth.providers.supabase.enabled=true",
      "auth.providers.supabase.jwt-secret=test-supabase-secret",
      "auth.providers.supabase.webhook-secret=test-webhook-secret"
    })
class SupabaseAuthWebhookIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthSessionRepository authSessionRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldRejectInvalidSecret() throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "type", "INSERT",
                "table", "users",
                "schema", "auth",
                "record", Map.of("id", "sub-1", "email", "a@example.com")));
    mockMvc
        .perform(
            post("/auth/webhooks/supabase")
                .header("X-Webhook-Secret", "wrong")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldInsertUpdateAndDelete() throws Exception {
    // INSERT
    mockMvc
        .perform(
            post("/auth/webhooks/supabase")
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "type",
                            "INSERT",
                            "table",
                            "users",
                            "schema",
                            "auth",
                            "record",
                            Map.of(
                                "id",
                                "sub-insert",
                                "email",
                                "ins@example.com",
                                "phone",
                                "+261330000111")))))
        .andExpect(status().isNoContent());

    UserAccount inserted =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, "sub-insert")
            .orElseThrow();
    Assertions.assertEquals("ins@example.com", inserted.getEmail());

    // UPDATE
    mockMvc
        .perform(
            post("/auth/webhooks/supabase")
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "type",
                            "UPDATE",
                            "table",
                            "users",
                            "schema",
                            "auth",
                            "record",
                            Map.of(
                                "id",
                                "sub-insert",
                                "email",
                                "updated@example.com",
                                "phone",
                                "+261330000222")))))
        .andExpect(status().isNoContent());

    UserAccount updated =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, "sub-insert")
            .orElseThrow();
    Assertions.assertEquals("updated@example.com", updated.getEmail());

    // DELETE
    mockMvc
        .perform(
            post("/auth/webhooks/supabase")
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "type",
                            "DELETE",
                            "table",
                            "users",
                            "schema",
                            "auth",
                            "old_record",
                            Map.of("id", "sub-insert")))))
        .andExpect(status().isNoContent());

    Assertions.assertTrue(
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, "sub-insert")
            .isEmpty());
  }

  @Test
  void shouldLinkExistingUserOnInsert() throws Exception {
    UserAccount existing =
        userRepository.save(
            UserAccount.builder()
                .firstname("Existing")
                .lastname("User")
                .email("link@example.com")
                .phone("+261330000999")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role(UserRole.SIMPLE_USER)
                .status("active")
                .build());

    mockMvc
        .perform(
            post("/auth/webhooks/supabase")
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "type",
                            "INSERT",
                            "table",
                            "users",
                            "schema",
                            "auth",
                            "record",
                            Map.of("id", "sub-link", "email", "link@example.com")))))
        .andExpect(status().isNoContent());

    UserAccount linked = userRepository.findById(existing.getId()).orElseThrow();
    Assertions.assertEquals(AuthProviderCode.SUPABASE, linked.getAuthProvider());
    Assertions.assertEquals("sub-link", linked.getExternalAuthId());
  }
}
