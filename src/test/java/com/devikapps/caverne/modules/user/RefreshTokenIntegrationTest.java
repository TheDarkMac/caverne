package com.devikapps.caverne.modules.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshTokenIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    registerUser();
  }

  @Test
  void shouldIssueRefreshAndCsrfCookiesOnLogin() throws Exception {
    MvcResult result = performLogin();

    Cookie refresh = result.getResponse().getCookie("refresh_token");
    Cookie csrf = result.getResponse().getCookie("csrf_token");
    assertNotNull(refresh, "refresh_token cookie must be set");
    assertNotNull(csrf, "csrf_token cookie must be set");
    org.junit.jupiter.api.Assertions.assertTrue(refresh.isHttpOnly());
    org.junit.jupiter.api.Assertions.assertFalse(csrf.isHttpOnly());
    assertEquals("/api/v1/auth", refresh.getPath());
  }

  @Test
  void shouldRotateRefreshOnRefreshCall() throws Exception {
    MvcResult login = performLogin();
    Cookie refresh = login.getResponse().getCookie("refresh_token");
    Cookie csrf = login.getResponse().getCookie("csrf_token");

    MvcResult refreshed =
        mockMvc
            .perform(
                post("/auth/refresh").header("X-CSRF-Token", csrf.getValue()).cookie(refresh, csrf))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isString())
            .andReturn();

    Cookie newRefresh = refreshed.getResponse().getCookie("refresh_token");
    Cookie newCsrf = refreshed.getResponse().getCookie("csrf_token");
    assertNotNull(newRefresh);
    assertNotNull(newCsrf);
    assertNotEquals(refresh.getValue(), newRefresh.getValue(), "refresh must rotate");
  }

  @Test
  void shouldRejectRefreshWithoutCsrfHeader() throws Exception {
    MvcResult login = performLogin();
    Cookie refresh = login.getResponse().getCookie("refresh_token");
    Cookie csrf = login.getResponse().getCookie("csrf_token");

    mockMvc.perform(post("/auth/refresh").cookie(refresh, csrf)).andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectRefreshWithMismatchedCsrf() throws Exception {
    MvcResult login = performLogin();
    Cookie refresh = login.getResponse().getCookie("refresh_token");
    Cookie csrf = login.getResponse().getCookie("csrf_token");

    mockMvc
        .perform(post("/auth/refresh").header("X-CSRF-Token", "wrong-value").cookie(refresh, csrf))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRevokeFamilyWhenRefreshReused() throws Exception {
    MvcResult login = performLogin();
    Cookie refresh = login.getResponse().getCookie("refresh_token");
    Cookie csrf = login.getResponse().getCookie("csrf_token");

    // First rotation succeeds.
    mockMvc
        .perform(
            post("/auth/refresh").header("X-CSRF-Token", csrf.getValue()).cookie(refresh, csrf))
        .andExpect(status().isOk());

    // Replaying the now-revoked refresh must fail AND revoke the whole family.
    mockMvc
        .perform(
            post("/auth/refresh").header("X-CSRF-Token", csrf.getValue()).cookie(refresh, csrf))
        .andExpect(status().isUnauthorized());

    long activeForUser =
        refreshTokenRepository.findAll().stream().filter(t -> t.getRevokedAt() == null).count();
    assertEquals(0, activeForUser, "every refresh in the family must be revoked after reuse");
  }

  @Test
  void shouldAcceptAccessTokenForProtectedRoutes() throws Exception {
    MvcResult login = performLogin();
    String access =
        org.openapitools.client.model.LoginResponse.fromJson(
                login.getResponse().getContentAsString())
            .getAccessToken();

    mockMvc
        .perform(get("/users/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("refresh@example.com"));
  }

  private void registerUser() {
    try {
      mockMvc
          .perform(
              post("/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "firstname", "Re",
                              "lastname", "Fresh",
                              "email", "refresh@example.com",
                              "phone", "+261340009999",
                              "password", "secret123"))))
          .andExpect(status().isCreated());
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private MvcResult performLogin() throws Exception {
    return mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("email", "refresh@example.com", "password", "secret123"))))
        .andExpect(status().isOk())
        .andReturn();
  }
}
