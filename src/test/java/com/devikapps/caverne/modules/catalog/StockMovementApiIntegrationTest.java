package com.devikapps.caverne.modules.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.order.OrderRepository;
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
class StockMovementApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthSessionRepository authSessionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private StockMovementRepository stockMovementRepository;
  @Autowired private OrderRepository orderRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    orderRepository.deleteAll();
    stockMovementRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldRecordStockMovementOnAdminAdjustment() throws Exception {
    String adminToken = loginAsAdmin("stockmove-admin@example.com", "secret123");
    Product product = saveProduct(10);

    mockMvc
        .perform(
            put("/products/{id}/stock", product.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", 25))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(25));

    String body =
        mockMvc
            .perform(
                get("/products/{id}/stock/movements", product.getId())
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].reason").value("MANUAL_ADJUSTMENT"))
            .andExpect(jsonPath("$.data[0].balance_after").value(25))
            .andExpect(jsonPath("$.data[0].delta").value(15))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode root = objectMapper.readTree(body);
    Assertions.assertEquals(1, root.path("meta").path("total").asInt());
  }

  @Test
  void shouldRejectNonAdminListingMovements() throws Exception {
    Product product = saveProduct(10);
    mockMvc
        .perform(get("/products/{id}/stock/movements", product.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldCreateOrderPlacedAndCancelledMovements() throws Exception {
    Product product = saveProduct(10);

    String orderPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 2)),
                "recipient",
                Map.of(
                    "location", "Antananarivo",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Buyer",
                    "recipient_email", "buyer@example.com",
                    "recipient_phone", "+261330000000")));

    String response =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderPayload))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID orderId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

    Assertions.assertEquals(
        BigDecimal.valueOf(8).intValue(),
        productRepository.findById(product.getId()).orElseThrow().getStockQuantity().intValue());

    mockMvc
        .perform(post("/orders/{id}/cancel", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));

    Assertions.assertEquals(
        10,
        productRepository.findById(product.getId()).orElseThrow().getStockQuantity().intValue());

    String adminToken = loginAsAdmin("stockmove-order@example.com", "secret123");
    mockMvc
        .perform(
            get("/products/{id}/stock/movements", product.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(2))
        .andExpect(jsonPath("$.data[0].reason").value("ORDER_CANCELLED"))
        .andExpect(jsonPath("$.data[1].reason").value("ORDER_PLACED"));
  }

  private Product saveProduct(int quantity) {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    Product product =
        Product.builder()
            .category(category)
            .label("Wild Pepper")
            .reference("PEP-MOV-" + UUID.randomUUID().toString().substring(0, 6))
            .description("desc")
            .size("1")
            .stockQuantity(BigDecimal.valueOf(quantity))
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
    UserAccount user =
        userRepository.save(
            UserAccount.builder()
                .firstname("Stock")
                .lastname("Admin")
                .email(email)
                .phone("+26134" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .status("active")
                .build());
    Assertions.assertNotNull(user.getId());
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
