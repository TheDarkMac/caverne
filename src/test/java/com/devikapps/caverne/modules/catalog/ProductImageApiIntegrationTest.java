package com.devikapps.caverne.modules.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
class ProductImageApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthSessionRepository authSessionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private ProductImageRepository productImageRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    productImageRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldCrudImagesAndEnforceSingleMain() throws Exception {
    String adminToken = loginAsAdmin("img-admin@example.com", "secret123");
    Product product = saveProduct();

    // Create first image as main
    String first =
        mockMvc
            .perform(
                post("/products/{pid}/images", product.getId())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("url", "https://cdn.test/a.jpg", "main", true))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.main").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID firstId = UUID.fromString(objectMapper.readTree(first).get("id").asText());

    // Create second image not main
    String second =
        mockMvc
            .perform(
                post("/products/{pid}/images", product.getId())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("url", "https://cdn.test/b.jpg", "main", false))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.main").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID secondId = UUID.fromString(objectMapper.readTree(second).get("id").asText());

    // Toggle main onto second
    mockMvc
        .perform(
            put("/products/{pid}/images/{iid}/main", product.getId(), secondId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.main").value(true));

    // List
    String listBody =
        mockMvc
            .perform(
                get("/products/{pid}/images", product.getId())
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode list = objectMapper.readTree(listBody);
    int mainCount = 0;
    for (JsonNode n : list) {
      if (n.get("main").asBoolean()) mainCount++;
    }
    Assertions.assertEquals(1, mainCount, "exactly one main image expected");

    // Delete first
    mockMvc
        .perform(
            delete("/products/{pid}/images/{iid}", product.getId(), firstId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    Assertions.assertFalse(productImageRepository.findById(firstId).isPresent());
  }

  @Test
  void shouldRejectAnonymousImageList() throws Exception {
    Product product = saveProduct();
    mockMvc
        .perform(get("/products/{pid}/images", product.getId()))
        .andExpect(status().isUnauthorized());
  }

  private Product saveProduct() {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    Product product =
        Product.builder()
            .category(category)
            .label("Wild Pepper")
            .reference("PEP-IMG-" + UUID.randomUUID().toString().substring(0, 6))
            .description("desc")
            .weight(BigDecimal.ONE)
            .length(BigDecimal.ONE)
            .width(BigDecimal.ONE)
            .height(BigDecimal.ONE)
            .stockQuantity(BigDecimal.valueOf(5))
            .isActive(true)
            .limitDate(LocalDate.of(2026, 12, 31))
            .build();
    Price price =
        Price.builder()
            .product(product)
            .currencyCode("MGA")
            .unit("kg")
            .validFrom(LocalDate.of(2026, 1, 1))
            .value(BigDecimal.valueOf(12000))
            .build();
    product.setPrices(List.of(price));
    return productRepository.save(product);
  }

  private String loginAsAdmin(String email, String password) throws Exception {
    userRepository.save(
        UserAccount.builder()
            .firstname("Image")
            .lastname("Admin")
            .email(email)
            .phone("+26134" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.ADMIN)
            .status("active")
            .build());
    String payload = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    String response =
        mockMvc
            .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("access_token").asText();
  }
}
