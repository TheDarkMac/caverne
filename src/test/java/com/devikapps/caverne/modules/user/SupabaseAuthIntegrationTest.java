package com.devikapps.caverne.modules.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
      "auth.providers.supabase.issuer=https://project-ref.supabase.co/auth/v1",
      "auth.providers.supabase.audience=authenticated"
    })
class SupabaseAuthIntegrationTest {

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
  void shouldLinkSupabaseTokenToExistingLocalUser() throws Exception {
    UserAccount existingUser =
        userRepository.save(
            UserAccount.builder()
                .firstname("Jean")
                .lastname("Rakoto")
                .email("jean@example.com")
                .phone("+261340000001")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role(UserRole.ADMIN)
                .status("active")
                .build());

    String token =
        createSupabaseJwt(
            "supabase-user-1",
            "jean@example.com",
            "+261340000001",
            Map.of("first_name", "Jean", "last_name", "Rakoto"));

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jean@example.com"))
        .andExpect(jsonPath("$.role").value("admin"));

    UserAccount linkedUser = userRepository.findById(existingUser.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(
        AuthProviderCode.SUPABASE, linkedUser.getAuthProvider());
    org.junit.jupiter.api.Assertions.assertEquals(
        "supabase-user-1", linkedUser.getExternalAuthId());
  }

  @Test
  void shouldProvisionSimpleUserFromSupabaseToken() throws Exception {
    String token =
        createSupabaseJwt(
            "supabase-user-2",
            "new-user@example.com",
            "+261340000777",
            Map.of("first_name", "New", "last_name", "User"));

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("new-user@example.com"))
        .andExpect(jsonPath("$.phone").value("+261340000777"))
        .andExpect(jsonPath("$.firstname").value("New"))
        .andExpect(jsonPath("$.lastname").value("User"))
        .andExpect(jsonPath("$.role").value("simple_user"));

    UserAccount createdUser =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, "supabase-user-2")
            .orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals("new-user@example.com", createdUser.getEmail());
  }

  @Test
  void shouldNotElevateProvisionedSupabaseUserToAdminFromTokenClaims() throws Exception {
    String token =
        createSupabaseJwt(
            "supabase-user-3",
            "admin-claim@example.com",
            "+261340000778",
            Map.of("first_name", "Claimed", "last_name", "Admin", "role", "admin"));

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin-claim@example.com"))
        .andExpect(jsonPath("$.role").value("simple_user"));

    UserAccount createdUser =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, "supabase-user-3")
            .orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(UserRole.SIMPLE_USER, createdUser.getRole());
  }

  @Test
  void shouldAllowSupabaseUserToCallLogout() throws Exception {
    String token =
        createSupabaseJwt(
            "supabase-user-logout",
            "logout-user@example.com",
            "+261340000555",
            Map.of("first_name", "Logout", "last_name", "User"));

    mockMvc
        .perform(
            post("/auth/logout")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  private String createSupabaseJwt(
      String subject, String email, String phone, Map<String, Object> userMetadata)
      throws Exception {
    String header = encode(Map.of("alg", "HS256", "typ", "JWT"));

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", subject);
    payload.put("email", email);
    payload.put("phone", phone);
    payload.put("iss", "https://project-ref.supabase.co/auth/v1");
    payload.put("aud", "authenticated");
    payload.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
    payload.put("iat", Instant.now().getEpochSecond());
    payload.put("user_metadata", userMetadata);

    String encodedPayload = encode(payload);
    String signature = sign(header + "." + encodedPayload);
    return header + "." + encodedPayload + "." + signature;
  }

  private String encode(Object value) throws Exception {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(objectMapper.writeValueAsBytes(value));
  }

  private String sign(String message) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(
        new SecretKeySpec("test-supabase-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
