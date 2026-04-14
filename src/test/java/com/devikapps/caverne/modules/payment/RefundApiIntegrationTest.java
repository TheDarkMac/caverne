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
import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderPayment;
import com.devikapps.caverne.modules.order.OrderRepository;
import com.devikapps.caverne.modules.order.OrderStatus;
import com.devikapps.caverne.modules.user.AuthSessionRepository;
import com.devikapps.caverne.modules.user.UserAccount;
import com.devikapps.caverne.modules.user.UserRepository;
import com.devikapps.caverne.modules.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
@Import({TestcontainersConfiguration.class, RefundApiIntegrationTest.MockRefundConfig.class})
@TestPropertySource(
    properties = {
      "stripe.enabled=true",
      "stripe.api-key=sk_test_placeholder",
      "spring.main.allow-bean-definition-overriding=true"
    })
class RefundApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthSessionRepository authSessionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldFullRefundWhenAmountOmitted() throws Exception {
    String adminToken = loginAsAdmin("refund-admin@example.com", "secret123");
    Order order = saveOrderWithStripePayment(BigDecimal.valueOf(20000));

    mockMvc
        .perform(
            post("/payments/{orderId}/refund", order.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund_id").value("re_mock_1"))
        .andExpect(jsonPath("$.refunded_amount").value(20000))
        .andExpect(jsonPath("$.payment_status").value("refunded"))
        .andExpect(jsonPath("$.order_status").value("CANCELLED"));

    Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
    Assertions.assertEquals(OrderStatus.CANCELLED, reloaded.getStatus());
  }

  @Test
  void shouldRejectRefundExceedingCaptured() throws Exception {
    String adminToken = loginAsAdmin("refund-admin2@example.com", "secret123");
    Order order = saveOrderWithStripePayment(BigDecimal.valueOf(5000));

    mockMvc
        .perform(
            post("/payments/{orderId}/refund", order.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 9999))))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void shouldNotTransitionDeliveredOrderToCancelledOnRefund() throws Exception {
    String adminToken = loginAsAdmin("refund-admin-delivered@example.com", "secret123");
    Order order = saveOrderWithStripePayment(BigDecimal.valueOf(12000), OrderStatus.DELIVERED);

    mockMvc
        .perform(
            post("/payments/{orderId}/refund", order.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund_id").value("re_mock_1"))
        .andExpect(jsonPath("$.payment_status").value("refunded"))
        .andExpect(jsonPath("$.order_status").value("DELIVERED"));

    Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
    Assertions.assertEquals(OrderStatus.DELIVERED, reloaded.getStatus());
  }

  @Test
  void shouldRejectAnonymousRefund() throws Exception {
    Order order = saveOrderWithStripePayment(BigDecimal.valueOf(5000));
    mockMvc
        .perform(
            post("/payments/{orderId}/refund", order.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  private Order saveOrderWithStripePayment(BigDecimal amount) {
    return saveOrderWithStripePayment(amount, OrderStatus.CONFIRMED);
  }

  private Order saveOrderWithStripePayment(BigDecimal amount, OrderStatus status) {
    Category cat =
        categoryRepository.save(Category.builder().label("Tea").slug("tea").map("TEA").build());
    Product product =
        productRepository.save(
            Product.builder()
                .category(cat)
                .label("Black Tea")
                .reference("REF-" + UUID.randomUUID().toString().substring(0, 6))
                .description("desc")
                .size("1")
                .stockQuantity(BigDecimal.valueOf(10))
                .isActive(true)
                .limitDate(LocalDate.of(2026, 12, 31))
                .build());
    Price price =
        Price.builder()
            .product(product)
            .currencyCode("MGA")
            .unit("box")
            .validFrom(LocalDate.of(2026, 1, 1))
            .value(BigDecimal.valueOf(5000))
            .build();
    product.setPrices(List.of(price));
    productRepository.save(product);

    Order order =
        Order.builder()
            .reference("ORD-RF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
            .date(LocalDateTime.now())
            .status(status)
            .currencyCode("MGA")
            .recipientName("Customer")
            .recipientEmail("cust@example.com")
            .recipientPhone("+261330000000")
            .shippingLocation("City")
            .postalCode("101")
            .countryCode("MDG")
            .totalAmount(amount)
            .build();
    OrderPayment payment =
        OrderPayment.builder()
            .paymentId(UUID.randomUUID())
            .order(order)
            .methodCode("STRIPE")
            .currencyCode("MGA")
            .amount(amount)
            .date(LocalDateTime.now())
            .status("confirmed")
            .internalReference("cs_test_for_refund")
            .build();
    order.getPayments().add(payment);
    return orderRepository.save(order);
  }

  private String loginAsAdmin(String email, String password) throws Exception {
    userRepository.save(
        UserAccount.builder()
            .firstname("Refund")
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

  @TestConfiguration
  static class MockRefundConfig {

    @Bean
    StripeRefundClient stripeRefundClient() {
      return (paymentRef, amount, currency, reason) ->
          new StripeRefundClient.RefundResult("re_mock_1", "succeeded");
    }

    @Bean("stripePaymentProvider")
    PaymentProvider stripePaymentProvider() {
      return new MockStripePaymentProvider();
    }
  }
}
