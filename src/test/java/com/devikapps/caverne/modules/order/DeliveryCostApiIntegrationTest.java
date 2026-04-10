package com.devikapps.caverne.modules.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.user.AuthSessionRepository;
import com.devikapps.caverne.modules.user.UserAccount;
import com.devikapps.caverne.modules.user.UserRepository;
import com.devikapps.caverne.modules.user.UserRole;
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
class DeliveryCostApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private DeliveryCostRepository deliveryCostRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthSessionRepository authSessionRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    deliveryCostRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldCreateListAndUpdateDeliveryCosts() throws Exception {
    String adminToken = loginAsAdmin("delivery-admin@example.com", "secret123");

    String createPayload =
        objectMapper.writeValueAsString(Map.of("amount", 5000, "provider", "STANDARD"));

    String response =
        mockMvc
            .perform(
                post("/delivery-costs")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(5000))
            .andExpect(jsonPath("$.provider").value("STANDARD"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Integer deliveryCostId = org.openapitools.client.model.DeliverCost.fromJson(response).getId();

    mockMvc
        .perform(get("/delivery-costs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(deliveryCostId))
        .andExpect(jsonPath("$[0].provider").value("STANDARD"));

    String updatePayload =
        objectMapper.writeValueAsString(Map.of("amount", 6500, "provider", "EXPRESS"));

    mockMvc
        .perform(
            put("/delivery-costs/{id}", deliveryCostId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(deliveryCostId))
        .andExpect(jsonPath("$.amount").value(6500))
        .andExpect(jsonPath("$.provider").value("EXPRESS"));
  }

  @Test
  void shouldRejectDeliveryCostWritesForSimpleUser() throws Exception {
    String token = loginAsSimpleUser("delivery-user@example.com", "secret123");
    String payload =
        objectMapper.writeValueAsString(Map.of("amount", 5000, "provider", "STANDARD"));

    mockMvc
        .perform(
            post("/delivery-costs")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());
  }

  private String loginAsAdmin(String email, String password) throws Exception {
    createUser(email, password, UserRole.ADMIN);
    return login(email, password);
  }

  private String loginAsSimpleUser(String email, String password) throws Exception {
    createUser(email, password, UserRole.SIMPLE_USER);
    return login(email, password);
  }

  private void createUser(String email, String password, UserRole role) {
    userRepository.save(
        UserAccount.builder()
            .firstname("Delivery")
            .lastname("User")
            .email(email)
            .phone("+26134" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
            .passwordHash(passwordEncoder.encode(password))
            .role(role)
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
