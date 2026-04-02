package com.devikapps.caverne.modules.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserAuthIntegrationTest {

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
  void shouldRegisterLoginAndFetchCurrentUser() throws Exception {
    String registerPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", "Jean",
                "lastname", "Rakoto",
                "email", "jean@example.com",
                "phone", "+261340000000",
                "password", "secret123"));

    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.firstname").value("Jean"))
        .andExpect(jsonPath("$.role").value("simple_user"))
        .andExpect(jsonPath("$.status").value("active"));

    String token = login("jean@example.com", "secret123");

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jean@example.com"))
        .andExpect(jsonPath("$.role").value("simple_user"));
  }

  @Test
  void shouldUpdateCurrentUserProfile() throws Exception {
    registerSimpleUser("mia@example.com", "Mia", "Ravo", "secret123");
    String token = login("mia@example.com", "secret123");

    String payload =
        objectMapper.writeValueAsString(
            Map.of("firstname", "Mialy", "lastname", "Rabe", "phone", "+261330000001"));

    mockMvc
        .perform(
            put("/users/me")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstname").value("Mialy"))
        .andExpect(jsonPath("$.lastname").value("Rabe"))
        .andExpect(jsonPath("$.phone").value("+261330000001"));
  }

  @Test
  void shouldAllowAdminToListGetAndDeleteUsers() throws Exception {
    registerSimpleUser("user1@example.com", "User", "One", "secret123");
    registerSimpleUser("user2@example.com", "User", "Two", "secret123");
    UserAccount admin = createAdmin("admin@example.com", "secret123");

    String adminToken = login("admin@example.com", "secret123");

    mockMvc
        .perform(
            get("/users")
                .header("Authorization", bearer(adminToken))
                .queryParam("role", "simple_user")
                .queryParam("page", "1")
                .queryParam("per_page", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(2))
        .andExpect(jsonPath("$.data[0].role").value("simple_user"));

    mockMvc
        .perform(get("/users/{id}", admin.getId()).header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@example.com"))
        .andExpect(jsonPath("$.role").value("admin"));

    UserAccount removable = userRepository.findByEmailIgnoreCase("user1@example.com").orElseThrow();

    mockMvc
        .perform(
            delete("/users/{id}", removable.getId()).header("Authorization", bearer(adminToken)))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldRejectAdminEndpointsForSimpleUser() throws Exception {
    registerSimpleUser("simple@example.com", "Simple", "User", "secret123");
    String token = login("simple@example.com", "secret123");

    mockMvc
        .perform(get("/users").header("Authorization", bearer(token)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldLogoutInvalidateCurrentToken() throws Exception {
    registerSimpleUser("logout@example.com", "Log", "Out", "secret123");
    String token = login("logout@example.com", "secret123");

    mockMvc
        .perform(post("/auth/logout").header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  private void registerSimpleUser(String email, String firstname, String lastname, String password)
      throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", firstname,
                "lastname", lastname,
                "email", email,
                "phone", "+261320000000",
                "password", password));

    mockMvc
        .perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated());
  }

  private UserAccount createAdmin(String email, String password) {
    return userRepository.save(
        UserAccount.builder()
            .firstname("Admin")
            .lastname("User")
            .email(email)
            .phone("+261340000999")
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.ADMIN)
            .status("active")
            .build());
  }

  private String login(String email, String password) throws Exception {
    String payload = objectMapper.writeValueAsString(Map.of("email", email, "password", password));

    String response =
        mockMvc
            .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return org.openapitools.client.model.LoginResponse.fromJson(response).getAccessToken();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
