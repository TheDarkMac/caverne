package com.devikapps.caverne.modules.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.catalog.Category;
import com.devikapps.caverne.modules.catalog.CategoryRepository;
import com.devikapps.caverne.modules.catalog.Price;
import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "stripe.enabled=true")
class RealStripePaymentIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductRepository productRepository;
  @Value("${RUN_REAL_STRIPE_TEST:false}")
  private boolean runRealStripeTest;

  @Value("${stripe.api-key:}")
  private String stripeApiKey;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(
        runRealStripeTest,
        "Set RUN_REAL_STRIPE_TEST=true to execute the real Stripe integration test.");
    Assumptions.assumeTrue(
        stripeApiKey != null
            && !stripeApiKey.isBlank(),
        "Set STRIPE_API_KEY in .env before running the real Stripe integration test.");
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void shouldCreateStripePaymentIntentWhenApiKeyIsValid() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    Product product =
        productRepository.save(
            product("Cardamom", "CAR-001", true, "Priced per kg", category, "MGA", "kg", 15000));
    Long orderId = createOrder(product);

    String payload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "STRIPE", "currency_code", "MGA", "amount", 15000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.method_code").value("STRIPE"))
        .andExpect(jsonPath("$.internal_reference").value(org.hamcrest.Matchers.startsWith("pi_")))
        .andExpect(jsonPath("$.provider_response.client_secret").isNotEmpty());
  }

  private Long createOrder(Product product) throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 1)),
                "recipient",
                Map.of(
                    "location", "Antananarivo",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Stripe Live",
                    "recipient_email", "realstripe@caverne.test",
                    "recipient_phone", "+261330000000")));
    String response =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private Product product(
      String label,
      String reference,
      boolean isActive,
      String description,
      Category category,
      String currencyCode,
      String unit,
      int amount) {
    Product product =
        Product.builder()
            .category(category)
            .label(label)
            .reference(reference)
            .description(description)
            .size(unit)
            .isActive(isActive)
            .limitDate(LocalDate.of(2026, 12, 31))
            .build();
    Price price =
        Price.builder()
            .product(product)
            .currencyCode(currencyCode)
            .unit(unit)
            .validFrom(LocalDate.of(2026, 1, 1))
            .value(BigDecimal.valueOf(amount))
            .build();
    product.setPrices(List.of(price));
    return product;
  }
}
