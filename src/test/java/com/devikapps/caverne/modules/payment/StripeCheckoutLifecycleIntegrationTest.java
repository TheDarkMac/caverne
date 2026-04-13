package com.devikapps.caverne.modules.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.catalog.Category;
import com.devikapps.caverne.modules.catalog.CategoryRepository;
import com.devikapps.caverne.modules.catalog.Price;
import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  StripeCheckoutLifecycleIntegrationTest.MockStripeLifecycleConfig.class
})
@TestPropertySource(
    properties = {
      "stripe.enabled=true",
      "stripe.api-key=sk_test_placeholder",
      "stripe.checkout-success-url=http://localhost:3000/checkout/success?session_id={CHECKOUT_SESSION_ID}",
      "stripe.checkout-cancel-url=http://localhost:3000/checkout/cancel",
      "stripe.webhook-secret=whsec_test_secret",
      "spring.main.allow-bean-definition-overriding=true"
    })
class StripeCheckoutLifecycleIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductRepository productRepository;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void shouldCreateOwnedOrderInitiateStripePaymentAndFinalizeThroughWebhook() throws Exception {
    Product product = saveProduct("Wild Pepper", "PEP-001", 18000);

    registerSimpleUser("lifecycle@example.com", "Life", "Cycle", "secret123");
    String token = login("lifecycle@example.com", "secret123");

    UUID orderId = createOwnedOrder(product, token);

    String paymentPayload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "STRIPE", "currency_code", "MGA", "amount", 18000));

    String paymentResponseBody =
        mockMvc
            .perform(
                post("/orders/{id}/payments", orderId)
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.method_code").value("STRIPE"))
            .andExpect(jsonPath("$.status").value("pending"))
            .andExpect(
                jsonPath("$.provider_response.checkout_url")
                    .value("https://checkout.stripe.test/session/cs_test_lifecycle_123"))
            .andExpect(
                jsonPath("$.provider_response.checkout_session_id").value("cs_test_lifecycle_123"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode paymentResponse = objectMapper.readTree(paymentResponseBody);
    String checkoutSessionId =
        paymentResponse.path("provider_response").path("checkout_session_id").asText();

    String webhookPayload =
        buildWebhookPayload("checkout.session.completed", checkoutSessionId, "complete", "paid");
    String signature = buildSignature(webhookPayload, "whsec_test_secret");

    mockMvc
        .perform(
            post("/payments/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signature)
                .content(webhookPayload))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/orders/{id}", orderId).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("confirmed"));

    mockMvc
        .perform(get("/orders/{id}/payments", orderId).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].method_code").value("STRIPE"))
        .andExpect(jsonPath("$[0].status").value("confirmed"))
        .andExpect(jsonPath("$[0].internal_reference").value(checkoutSessionId))
        .andExpect(jsonPath("$[0].provider_response.checkout_session_id").value(checkoutSessionId))
        .andExpect(jsonPath("$[0].provider_response.checkout_status").value("complete"))
        .andExpect(jsonPath("$[0].provider_response.payment_status").value("paid"))
        .andExpect(
            jsonPath("$[0].provider_response.last_webhook_event")
                .value("checkout.session.completed"))
        .andExpect(jsonPath("$[0].provider_response.webhook_processed_at").exists());
  }

  private Product saveProduct(String label, String reference, int amount) {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    Product product =
        Product.builder()
            .category(category)
            .label(label)
            .reference(reference)
            .description(label + " description")
            .size("unit")
            .stockQuantity(BigDecimal.valueOf(20))
            .isActive(true)
            .limitDate(LocalDate.of(2026, 12, 31))
            .build();

    Price price =
        Price.builder()
            .product(product)
            .currencyCode("MGA")
            .unit("unit")
            .validFrom(LocalDate.of(2026, 1, 1))
            .value(BigDecimal.valueOf(amount))
            .build();

    product.setPrices(List.of(price));
    return productRepository.save(product);
  }

  private void registerSimpleUser(String email, String firstname, String lastname, String password)
      throws Exception {
    String phone = "+26132" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000);
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "firstname", firstname,
                "lastname", lastname,
                "email", email,
                "phone", phone,
                "password", password));

    mockMvc
        .perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated());
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

    return objectMapper.readTree(response).get("access_token").asText();
  }

  private UUID createOwnedOrder(Product product, String token) throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 1)),
                "recipient",
                Map.of(
                    "location", "Analakely",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Life Cycle",
                    "recipient_email", "lifecycle@example.com",
                    "recipient_phone", "+261340000111")));

    String response =
        mockMvc
            .perform(
                post("/orders")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private String buildWebhookPayload(
      String eventType, String sessionId, String checkoutStatus, String paymentStatus)
      throws Exception {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("id", "evt_lifecycle_123");
    root.put("object", "event");
    root.put("type", eventType);

    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, Object> object = new LinkedHashMap<>();
    object.put("id", sessionId);
    object.put("object", "checkout.session");
    object.put("status", checkoutStatus);
    object.put("payment_status", paymentStatus);
    data.put("object", object);
    root.put("data", data);
    return objectMapper.writeValueAsString(root);
  }

  private String buildSignature(String payload, String secret) throws Exception {
    long timestamp = System.currentTimeMillis() / 1000L;
    String signedPayload = timestamp + "." + payload;
    String signature = Webhook.Util.computeHmacSha256(secret, signedPayload);
    return "t=" + timestamp + ",v1=" + signature;
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  @TestConfiguration
  static class MockStripeLifecycleConfig {

    @Bean("stripePaymentProvider")
    PaymentProvider stripePaymentProvider() {
      return new PaymentProvider() {
        @Override
        public String getProviderCode() {
          return "STRIPE";
        }

        @Override
        public PaymentResponse initiatePayment(
            BigDecimal amount, String currency, String orderReference) {
          return new PaymentResponse(
              "cs_test_lifecycle_123",
              "pending",
              "https://checkout.stripe.test/session/cs_test_lifecycle_123",
              Map.of(
                  "checkout_url", "https://checkout.stripe.test/session/cs_test_lifecycle_123",
                  "checkout_session_id", "cs_test_lifecycle_123",
                  "checkout_status", "open",
                  "payment_status", "unpaid"));
        }
      };
    }
  }
}
