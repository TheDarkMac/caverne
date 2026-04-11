package com.devikapps.caverne.modules.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.devikapps.caverne.modules.order.Order;
import com.devikapps.caverne.modules.order.OrderPayment;
import com.devikapps.caverne.modules.order.OrderRepository;
import com.devikapps.caverne.modules.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
    properties = {
      "stripe.enabled=true",
      "stripe.api-key=sk_test_placeholder",
      "stripe.checkout-success-url=http://localhost:3000/checkout/success?session_id={CHECKOUT_SESSION_ID}",
      "stripe.checkout-cancel-url=http://localhost:3000/checkout/cancel",
      "stripe.webhook-secret=whsec_test_secret"
    })
class StripeWebhookIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrderRepository orderRepository;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
  }

  @Test
  void shouldConfirmStripePaymentAndOrderFromWebhook() throws Exception {
    Order order =
        orderRepository.save(
            Order.builder()
                .reference("ORD-WEBHOOK-1")
                .date(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .currencyCode("MGA")
                .recipientName("Webhook User")
                .recipientEmail("webhook@example.com")
                .recipientPhone("+261340000999")
                .shippingLocation("Analakely")
                .postalCode("101")
                .countryCode("MDG")
                .totalAmount(BigDecimal.valueOf(15000))
                .payments(
                    List.of(
                        OrderPayment.builder()
                            .paymentId(UUID.randomUUID())
                            .methodCode("STRIPE")
                            .currencyCode("MGA")
                            .amount(BigDecimal.valueOf(15000))
                            .date(LocalDateTime.now())
                            .status("pending")
                            .internalReference("cs_test_session_123")
                            .providerResponse(
                                """
{"checkout_session_id":"cs_test_session_123","checkout_status":"open","payment_status":"unpaid"}
""")
                            .build()))
                .build());

    String payload =
        buildPayload("checkout.session.completed", "cs_test_session_123", "complete", "paid");
    String signature = buildSignature(payload, "whsec_test_secret");

    mockMvc
        .perform(
            post("/payments/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signature)
                .content(payload))
        .andExpect(status().isNoContent());

    Order updated = orderRepository.findById(order.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, updated.getStatus());
    org.junit.jupiter.api.Assertions.assertEquals(
        "confirmed", updated.getPayments().getFirst().getStatus());
    org.junit.jupiter.api.Assertions.assertTrue(
        updated
            .getPayments()
            .getFirst()
            .getProviderResponse()
            .contains("\"last_webhook_event\":\"checkout.session.completed\""));
  }

  private String buildPayload(
      String eventType, String sessionId, String checkoutStatus, String paymentStatus)
      throws Exception {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("id", "evt_test_123");
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
}
