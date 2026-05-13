package com.devikapps.caverne.modules.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.catalog.Category;
import com.devikapps.caverne.modules.catalog.CategoryRepository;
import com.devikapps.caverne.modules.catalog.Price;
import com.devikapps.caverne.modules.catalog.Product;
import com.devikapps.caverne.modules.catalog.ProductRepository;
import com.devikapps.caverne.modules.payment.MockStripePaymentProvider;
import com.devikapps.caverne.modules.payment.PaymentProvider;
import com.devikapps.caverne.modules.user.RefreshTokenRepository;
import com.devikapps.caverne.modules.user.UserAccount;
import com.devikapps.caverne.modules.user.UserRepository;
import com.devikapps.caverne.modules.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CheckoutApiIntegrationTest.StripeTestConfig.class})
@TestPropertySource(properties = {"stripe.enabled=false", "manual.enabled=true"})
class CheckoutApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private DeliveryCostRepository deliveryCostRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    orderRepository.deleteAll();
    deliveryCostRepository.deleteAll();
    userRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void shouldCreateOrderFromRecipientPayload() throws Exception {
    Product product = saveProduct("Arabica Coffee", "COF-001", 25000, 10);
    DeliveryCost deliveryCost =
        deliveryCostRepository.save(
            DeliveryCost.builder().amount(BigDecimal.valueOf(5000)).provider("STANDARD").build());

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "delivery_cost_id",
                deliveryCost.getId(),
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 2)),
                "recipient",
                Map.of(
                    "location", "Lot II M 10 Antananarivo",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Jean Rakoto",
                    "recipient_email", "jean@example.com",
                    "recipient_phone", "+261340000000")));

    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("pending"))
        .andExpect(jsonPath("$.currency_code").value("MGA"))
        .andExpect(jsonPath("$.delivery_cost_id").value(deliveryCost.getId().toString()))
        .andExpect(jsonPath("$.total_amount").value(55000))
        .andExpect(jsonPath("$.items[0].product_id").value(product.getId().toString()))
        .andExpect(jsonPath("$.items[0].product.label").value("Arabica Coffee"))
        .andExpect(jsonPath("$.items[0].product.prices[0].value").value(25000))
        .andExpect(jsonPath("$.items[0].unit_price").value(25000))
        .andExpect(jsonPath("$.items[0].total_price").value(50000))
        .andExpect(jsonPath("$.recipient.recipient_email").value("jean@example.com"));
  }

  @Test
  void shouldInitiateManualPaymentForOrder() throws Exception {
    Product product = saveProduct("Cloves", "CLO-001", 12000, 10);
    UUID orderId = createOrder(product);

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "method_code", "MANUAL",
                "currency_code", "MGA",
                "amount", 12000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.method_code").value("MANUAL"))
        .andExpect(jsonPath("$.currency_code").value("MGA"))
        .andExpect(jsonPath("$.status").value("pending"))
        .andExpect(jsonPath("$.provider_response.instructions").exists());

    mockMvc
        .perform(get("/orders/{id}/payments", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].method_code").value("MANUAL"));
  }

  @Test
  void shouldInitiatePaymentWithMockStripeProvider() throws Exception {
    Product product = saveProduct("Lavender", "LAV-001", 8000, 10);
    UUID orderId = createOrder(product);

    String payload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "STRIPE", "currency_code", "MGA", "amount", 8000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.method_code").value("STRIPE"))
        .andExpect(jsonPath("$.currency_code").value("MGA"))
        .andExpect(jsonPath("$.status").value("pending"))
        .andExpect(
            jsonPath("$.provider_response.checkout_url")
                .value(
                    org.hamcrest.Matchers.startsWith(
                        "https://checkout.stripe.test/session/mock-ORD-")))
        .andExpect(jsonPath("$.provider_response.checkout_session_id").exists())
        .andExpect(jsonPath("$.provider_response.order_reference").exists());
  }

  @Test
  void shouldUseComputedOrderTotalIncludingDeliveryCostForStripePayment() throws Exception {
    Product product = saveProduct("Pepper", "PEP-DELIVERY", 25000, 10);
    DeliveryCost deliveryCost =
        deliveryCostRepository.save(
            DeliveryCost.builder().amount(BigDecimal.valueOf(5000)).provider("STANDARD").build());

    String orderPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "delivery_cost_id",
                deliveryCost.getId(),
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 1)),
                "recipient",
                Map.of(
                    "location", "Analakely",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Guest Buyer",
                    "recipient_email", "guest@example.com",
                    "recipient_phone", "+261340000000")));

    String orderResponse =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(orderPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total_amount").value(30000))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID orderId = UUID.fromString(objectMapper.readTree(orderResponse).get("id").asText());

    String paymentPayload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "STRIPE", "currency_code", "MGA", "amount", 30000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amount").value(30000))
        .andExpect(jsonPath("$.provider_response.checkout_url").exists());

    String wrongAmountPayload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "STRIPE", "currency_code", "MGA", "amount", 25000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongAmountPayload))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void shouldUpdateOrderStatusUsingContractPayload() throws Exception {
    Product product = saveProduct("Cinnamon", "CIN-001", 9000, 10);
    UUID orderId = createOrder(product);
    String adminToken = loginAsAdmin("admin-status@example.com", "secret123");

    String payload = objectMapper.writeValueAsString(Map.of("status", "confirmed"));

    mockMvc
        .perform(
            put("/orders/{id}/status", orderId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("confirmed"));
  }

  @Test
  void shouldListAllOrdersWithStatusFilter() throws Exception {
    Product product = saveProduct("Vanilla", "VAN-002", 15000, 10);
    UUID pendingOrderId = createOrder(product);
    UUID confirmedOrderId = createOrder(product);
    String adminToken = loginAsAdmin("admin-list@example.com", "secret123");

    String statusPayload = objectMapper.writeValueAsString(Map.of("status", "confirmed"));

    mockMvc
        .perform(
            put("/orders/{id}/status", confirmedOrderId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusPayload))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/orders/all")
                .header("Authorization", bearer(adminToken))
                .queryParam("status", "confirmed")
                .queryParam("page", "1")
                .queryParam("per_page", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].id").value(confirmedOrderId.toString()))
        .andExpect(jsonPath("$.data[0].status").value("confirmed"));

    mockMvc
        .perform(get("/orders/{id}", pendingOrderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(pendingOrderId.toString()));
  }

  @Test
  void shouldCancelOrder() throws Exception {
    Product product = saveProduct("Ginger", "GIN-001", 7000, 10);
    UUID orderId = createOrder(product);

    mockMvc
        .perform(post("/orders/{id}/cancel", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("cancelled"));
  }

  @Test
  void shouldCreateOwnedOrderAndExposeOnlyCurrentUserHistory() throws Exception {
    Product product = saveProduct("Baobab", "BAO-001", 11000, 10);
    registerSimpleUser("owner@example.com", "Owner", "User", "secret123");
    registerSimpleUser("other@example.com", "Other", "User", "secret123");
    String ownerToken = login("owner@example.com", "secret123");
    String otherToken = login("other@example.com", "secret123");
    String adminToken = loginAsAdmin("admin-owner@example.com", "secret123");

    UUID ownedOrderId = createOrder(product, ownerToken);
    UUID guestOrderId = createOrder(product);

    mockMvc
        .perform(
            get("/orders")
                .header("Authorization", bearer(ownerToken))
                .queryParam("page", "1")
                .queryParam("per_page", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].id").value(ownedOrderId.toString()))
        .andExpect(jsonPath("$.data[0].user_id").exists());

    mockMvc
        .perform(get("/orders/{id}", ownedOrderId).header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownedOrderId.toString()))
        .andExpect(jsonPath("$.user_id").exists());

    mockMvc
        .perform(get("/orders/{id}", ownedOrderId).header("Authorization", bearer(otherToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/orders/{id}/payments", ownedOrderId).header("Authorization", bearer(otherToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/orders/{id}", ownedOrderId).header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownedOrderId.toString()));

    String manualPaymentPayload =
        objectMapper.writeValueAsString(
            Map.of("method_code", "MANUAL", "currency_code", "MGA", "amount", 11000));

    mockMvc
        .perform(
            post("/orders/{id}/payments", ownedOrderId)
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualPaymentPayload))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/orders/{id}/payments", ownedOrderId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualPaymentPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.method_code").value("MANUAL"));

    mockMvc
        .perform(
            get("/orders/{id}/payments", ownedOrderId).header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].method_code").value("MANUAL"));

    mockMvc
        .perform(
            get("/orders/all")
                .header("Authorization", bearer(adminToken))
                .queryParam("user_id", findUserByEmail("owner@example.com").getId().toString())
                .queryParam("page", "1")
                .queryParam("per_page", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].id").value(ownedOrderId.toString()));

    mockMvc
        .perform(
            post("/orders/{id}/cancel", ownedOrderId).header("Authorization", bearer(otherToken)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/orders/{id}/cancel", ownedOrderId).header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));

    mockMvc
        .perform(get("/orders/{id}", guestOrderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user_id").doesNotExist());
  }

  @Test
  void shouldUseNewestApplicablePriceAndPreserveProductSnapshotOnOrder() throws Exception {
    Product product =
        saveProduct(
            "Historic Vanilla",
            "HIS-001",
            List.of(
                price("MGA", "unit", 10000, LocalDate.of(2025, 1, 1)),
                price("MGA", "unit", 14000, LocalDate.of(2026, 1, 1))),
            5);

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 2)),
                "recipient",
                Map.of(
                    "location", "Analakely",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Jean Rakoto",
                    "recipient_email", "jean@example.com",
                    "recipient_phone", "+261340000000")));

    String response =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total_amount").value(28000))
            .andExpect(jsonPath("$.items[0].unit_price").value(14000))
            .andExpect(jsonPath("$.items[0].total_price").value(28000))
            .andExpect(jsonPath("$.items[0].product.prices[0].value").value(14000))
            .andReturn()
            .getResponse()
            .getContentAsString();

    product.getPrices().add(priceEntity(product, "MGA", "unit", 17000, LocalDate.of(2026, 6, 1)));
    productRepository.save(product);

    UUID orderId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

    mockMvc
        .perform(get("/orders/{id}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].product.label").value("Historic Vanilla"))
        .andExpect(jsonPath("$.items[0].product.prices[0].value").value(14000))
        .andExpect(jsonPath("$.items[0].unit_price").value(14000))
        .andExpect(jsonPath("$.items[0].total_price").value(28000));
  }

  @Test
  void shouldRejectOrderWhenRequestedQuantityExceedsStockAndDecrementOnSuccess() throws Exception {
    Product product = saveProduct("Stocked Pepper", "STK-001", 12000, 2);

    String tooLargePayload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 3)),
                "recipient",
                Map.of(
                    "location", "Analakely",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Jean Rakoto",
                    "recipient_email", "jean@example.com",
                    "recipient_phone", "+261340000000")));

    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(tooLargePayload))
        .andExpect(status().isUnprocessableEntity());

    String validPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code",
                "MGA",
                "items",
                List.of(Map.of("product_id", product.getId(), "quantity", 2)),
                "recipient",
                Map.of(
                    "location", "Analakely",
                    "postal_code", "101",
                    "country_code", "MDG",
                    "recipient_name", "Jean Rakoto",
                    "recipient_email", "jean@example.com",
                    "recipient_phone", "+261340000000")));

    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(validPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items[0].total_price").value(24000));

    Product updated = productRepository.findById(product.getId()).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(updated.getStockQuantity())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  private Product saveProduct(String label, String reference, int amount, double stockQuantity) {
    return saveProduct(
        label,
        reference,
        List.of(price("MGA", "unit", amount, LocalDate.of(2026, 1, 1))),
        stockQuantity);
  }

  private Product saveProduct(
      String label, String reference, List<Price> prices, double stockQuantity) {
    Category category =
        categoryRepository.save(
            Category.builder().label("Default").slug("default").map("DEFAULT").build());

    Product product =
        Product.builder()
            .category(category)
            .label(label)
            .reference(reference)
            .description(label + " description")
            .weight(BigDecimal.ONE)
            .length(BigDecimal.ONE)
            .width(BigDecimal.ONE)
            .height(BigDecimal.ONE)
            .stockQuantity(BigDecimal.valueOf(stockQuantity))
            .isActive(true)
            .limitDate(LocalDate.of(2026, 12, 31))
            .build();

    prices.forEach(price -> price.setProduct(product));
    product.setPrices(new ArrayList<>(prices));
    return productRepository.save(product);
  }

  private Price price(String currency, String unit, double amount, LocalDate validFrom) {
    return Price.builder()
        .currencyCode(currency)
        .unit(unit)
        .validFrom(validFrom)
        .value(BigDecimal.valueOf(amount))
        .build();
  }

  private Price priceEntity(
      Product product, String currency, String unit, double amount, LocalDate validFrom) {
    return Price.builder()
        .product(product)
        .currencyCode(currency)
        .unit(unit)
        .validFrom(validFrom)
        .value(BigDecimal.valueOf(amount))
        .build();
  }

  private UUID createOrder(Product product) throws Exception {
    return createOrder(product, null);
  }

  private UUID createOrder(Product product, String token) throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code", "MGA",
                "items", List.of(Map.of("product_id", product.getId(), "quantity", 1)),
                "recipient",
                    Map.of(
                        "location", "Analakely",
                        "postal_code", "101",
                        "country_code", "MDG",
                        "recipient_name", "Jean Rakoto",
                        "recipient_email", "jean@example.com",
                        "recipient_phone", "+261340000000")));

    var request = post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload);
    if (token != null) {
      request.header("Authorization", bearer(token));
    }

    String response =
        mockMvc
            .perform(request)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
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

  private String loginAsAdmin(String email, String password) throws Exception {
    createAdmin(email, password);
    return login(email, password);
  }

  private UserAccount createAdmin(String email, String password) {
    return userRepository.save(
        UserAccount.builder()
            .firstname("Admin")
            .lastname("User")
            .email(email)
            .phone("+26134" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.ADMIN)
            .status("active")
            .build());
  }

  private UserAccount findUserByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(email).orElseThrow();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  @TestConfiguration
  static class StripeTestConfig {

    @Bean
    PaymentProvider stripePaymentProvider() {
      return new MockStripePaymentProvider();
    }
  }
}
