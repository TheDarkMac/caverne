package com.devikapps.caverne.modules.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
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
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
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
  void shouldRegisterWithPhoneOnlyAndLoginWithPhone() throws Exception {
    String registerPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", "Solo",
                "lastname", "Phone",
                "phone", "+261341111111",
                "password", "secret123"));

    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.firstname").value("Solo"))
        .andExpect(jsonPath("$.phone").value("+261341111111"))
        .andExpect(jsonPath("$.role").value("simple_user"));

    String token = loginByPhone("+261341111111", "secret123");

    mockMvc
        .perform(get("/users/me").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phone").value("+261341111111"))
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
  void shouldAllowAdminToCreateUser() throws Exception {
    createAdmin("creator@example.com", "secret123");
    String adminToken = login("creator@example.com", "secret123");

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", "Backoffice",
                "lastname", "Admin",
                "email", "backoffice@example.com",
                "phone", "+261340001111",
                "password", "secret123",
                "role", "admin",
                "status", "active"));

    mockMvc
        .perform(
            post("/users")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("backoffice@example.com"))
        .andExpect(jsonPath("$.role").value("admin"))
        .andExpect(jsonPath("$.status").value("active"));
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
  void shouldLogoutClearCookies() throws Exception {
    registerSimpleUser("logout@example.com", "Log", "Out", "secret123");
    org.springframework.test.web.servlet.MvcResult loginResult =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("email", "logout@example.com", "password", "secret123"))))
            .andExpect(status().isOk())
            .andReturn();

    jakarta.servlet.http.Cookie refreshCookie =
        loginResult.getResponse().getCookie("refresh_token");
    jakarta.servlet.http.Cookie csrfCookie = loginResult.getResponse().getCookie("csrf_token");
    org.junit.jupiter.api.Assertions.assertNotNull(refreshCookie);
    org.junit.jupiter.api.Assertions.assertNotNull(csrfCookie);

    String token =
        org.openapitools.client.model.LoginResponse.fromJson(
                loginResult.getResponse().getContentAsString())
            .getAccessToken();

    mockMvc
        .perform(
            post("/auth/logout")
                .header("Authorization", bearer(token))
                .header("X-CSRF-Token", csrfCookie.getValue())
                .cookie(refreshCookie, csrfCookie))
        .andExpect(status().isNoContent())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                .maxAge("refresh_token", 0));
  }

  @Test
  void shouldManageCurrentUserAddresses() throws Exception {
    registerSimpleUser("address@example.com", "Address", "Owner", "secret123");
    String token = login("address@example.com", "secret123");

    String firstAddressPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "location", "Lot II M 12 Analakely",
                "postal_code", "101",
                "country_code", "MG",
                "is_default", true));

    String firstResponse =
        mockMvc
            .perform(
                post("/users/me/addresses")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(firstAddressPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.location").value("Lot II M 12 Analakely"))
            .andExpect(jsonPath("$.is_default").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    java.util.UUID firstAddressId =
        org.openapitools.client.model.Address.fromJson(firstResponse).getId();

    String secondAddressPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "location", "Lot III F 20 Itaosy",
                "postal_code", "102",
                "country_code", "mg",
                "is_default", false));

    String secondResponse =
        mockMvc
            .perform(
                post("/users/me/addresses")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secondAddressPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.is_default").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

    java.util.UUID secondAddressId =
        org.openapitools.client.model.Address.fromJson(secondResponse).getId();

    String listedAddresses =
        mockMvc
            .perform(get("/users/me/addresses").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertAddressState(listedAddresses, firstAddressId, true, "MG");
    assertAddressState(listedAddresses, secondAddressId, false, "MG");

    String updatePayload =
        objectMapper.writeValueAsString(
            Map.of(
                "location", "Lot III F 21 Itaosy",
                "postal_code", "102",
                "country_code", "MG",
                "is_default", false));

    mockMvc
        .perform(
            put("/users/me/addresses/{id}", secondAddressId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location").value("Lot III F 21 Itaosy"))
        .andExpect(jsonPath("$.is_default").value(false));

    mockMvc
        .perform(
            put("/users/me/addresses/{id}/default", secondAddressId)
                .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(secondAddressId.toString()))
        .andExpect(jsonPath("$.is_default").value(true));

    String updatedAddresses =
        mockMvc
            .perform(get("/users/me/addresses").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertAddressState(updatedAddresses, firstAddressId, false, "MG");
    assertAddressState(updatedAddresses, secondAddressId, true, "MG");

    String postUpdatePayload =
        objectMapper.writeValueAsString(
            Map.of(
                "id", firstAddressId,
                "location", "Lot II M 99 Analakely",
                "postal_code", "101",
                "country_code", "MG",
                "is_default", false));

    mockMvc
        .perform(
            post("/users/me/addresses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(postUpdatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(firstAddressId.toString()))
        .andExpect(jsonPath("$.location").value("Lot II M 99 Analakely"));

    UUID newAddressId = UUID.randomUUID();
    String putCreatePayload =
        objectMapper.writeValueAsString(
            Map.of(
                "id", newAddressId,
                "location", "Lot IV A 40 Ivandry",
                "postal_code", "103",
                "country_code", "MG",
                "is_default", false));

    mockMvc
        .perform(
            put("/users/me/addresses/{id}", newAddressId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(putCreatePayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.location").value("Lot IV A 40 Ivandry"));

    mockMvc
        .perform(
            delete("/users/me/addresses/{id}", secondAddressId)
                .header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    String remainingAddresses =
        mockMvc
            .perform(get("/users/me/addresses").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertContainsAddress(remainingAddresses, firstAddressId, "MG");
    assertSingleDefaultAddress(remainingAddresses);
  }

  private void registerSimpleUser(String email, String firstname, String lastname, String password)
      throws Exception {
    String generatedPhone =
        "+26132" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000);
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", firstname,
                "lastname", lastname,
                "email", email,
                "phone", generatedPhone,
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

  private String loginByPhone(String phone, String password) throws Exception {
    String payload = objectMapper.writeValueAsString(Map.of("phone", phone, "password", password));

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

  private void assertAddressState(
      String addressesJson, UUID addressId, boolean isDefault, String countryCode)
      throws Exception {
    JsonNode address = findAddress(addressesJson, addressId);
    org.junit.jupiter.api.Assertions.assertNotNull(address);
    org.junit.jupiter.api.Assertions.assertEquals(
        isDefault, address.path("is_default").asBoolean());
    org.junit.jupiter.api.Assertions.assertEquals(
        countryCode, address.path("country_code").asText());
  }

  private void assertContainsAddress(String addressesJson, UUID addressId, String countryCode)
      throws Exception {
    JsonNode address = findAddress(addressesJson, addressId);
    org.junit.jupiter.api.Assertions.assertNotNull(address);
    org.junit.jupiter.api.Assertions.assertEquals(
        countryCode, address.path("country_code").asText());
  }

  private void assertSingleDefaultAddress(String addressesJson) throws Exception {
    JsonNode addresses = objectMapper.readTree(addressesJson);
    int defaultCount = 0;
    for (JsonNode candidate : addresses) {
      if (candidate.path("is_default").asBoolean()) {
        defaultCount++;
      }
    }
    org.junit.jupiter.api.Assertions.assertEquals(1, defaultCount);
  }

  private JsonNode findAddress(String addressesJson, UUID addressId) throws Exception {
    JsonNode addresses = objectMapper.readTree(addressesJson);
    for (JsonNode candidate : addresses) {
      if (addressId.toString().equals(candidate.path("id").asText())) {
        return candidate;
      }
    }
    return null;
  }
}
