package com.devikapps.caverne.modules.order;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CheckoutApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private OrderRepository orderRepository;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void shouldCreateGuestOrderFromCheckoutPayload() throws Exception {
    Product product = saveProduct("Arabica Coffee", "COF-001", 25000);

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code", "MGA",
                "items", List.of(Map.of("product_id", product.getId(), "quantity", 2)),
                "guest_address",
                    Map.of(
                        "location", "Lot II M 10 Antananarivo",
                        "postal_code", "101",
                        "country_code", "MDG",
                        "customer_name", "Jean Rakoto",
                        "customer_email", "jean@example.com",
                        "customer_phone", "+261340000000")));

    mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("pending"))
        .andExpect(jsonPath("$.currency_code").value("MGA"))
        .andExpect(jsonPath("$.items[0].product_id").value(product.getId()))
        .andExpect(jsonPath("$.items[0].unit_price").value(25000))
        .andExpect(jsonPath("$.guest_address.customer_email").value("jean@example.com"));
  }

  @Test
  void shouldInitiateManualPaymentForOrder() throws Exception {
    Product product = saveProduct("Cloves", "CLO-001", 12000);
    Long orderId = createOrder(product);

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

  private Product saveProduct(String label, String reference, int amount) {
    Category category =
        categoryRepository.save(
            Category.builder().label("Default").slug("default").map("DEFAULT").build());

    Product product =
        Product.builder()
            .category(category)
            .label(label)
            .reference(reference)
            .description(label + " description")
            .size("unit")
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

  private Long createOrder(Product product) throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "currency_code", "MGA",
                "items", List.of(Map.of("product_id", product.getId(), "quantity", 1)),
                "guest_address",
                    Map.of(
                        "location", "Analakely",
                        "postal_code", "101",
                        "country_code", "MDG",
                        "customer_name", "Jean Rakoto",
                        "customer_email", "jean@example.com",
                        "customer_phone", "+261340000000")));

    String response =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }
}
