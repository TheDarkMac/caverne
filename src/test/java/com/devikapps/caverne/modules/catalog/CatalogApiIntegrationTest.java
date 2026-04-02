package com.devikapps.caverne.modules.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
class CatalogApiIntegrationTest {

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
  void shouldListCategoriesAsTree() throws Exception {
    Category root =
        categoryRepository.save(Category.builder().label("Tea").slug("tea").map("TEA").build());

    categoryRepository.save(
        Category.builder().label("Green Tea").slug("green-tea").map("GREEN").parent(root).build());

    mockMvc
        .perform(get("/categories").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Tea"))
        .andExpect(jsonPath("$[0].children[0].label").value("Green Tea"))
        .andExpect(jsonPath("$[0].children[0].parent_id").value(root.getId()));
  }

  @Test
  void shouldListProductsWithPaginationAndFilters() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    productRepository.saveAll(
        List.of(
            product("Wild Pepper", "PEP-001", true, "Hot spice", category, "MGA", "kg", 12000),
            product(
                "Smoked Pepper",
                "PEP-002",
                false,
                "Inactive product",
                category,
                "MGA",
                "kg",
                13500),
            product("Vanilla", "VAN-001", true, "Aromatic vanilla", category, "EUR", "unit", 8)));

    mockMvc
        .perform(
            get("/products")
                .queryParam("search", "pepper")
                .queryParam("is_active", "true")
                .queryParam("page", "1")
                .queryParam("per_page", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.meta.page").value(1))
        .andExpect(jsonPath("$.data[0].label").value("Wild Pepper"))
        .andExpect(jsonPath("$.data[0].category_id").value(category.getId()))
        .andExpect(jsonPath("$.data[0].prices[0].currency_code").value("MGA"));
  }

  @Test
  void shouldReturnProductDetail() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Honey").slug("honey").map("HONEY").build());

    Product product =
        productRepository.save(
            product("Forest Honey", "HON-001", true, "Raw honey", category, "MGA", "jar", 18000));

    mockMvc
        .perform(get("/products/{id}", product.getId()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId()))
        .andExpect(jsonPath("$.label").value("Forest Honey"))
        .andExpect(jsonPath("$.reference").value("HON-001"))
        .andExpect(jsonPath("$.prices[0].value").value(18000));
  }

  @Test
  void shouldCreateCategoryUsingContractPayload() throws Exception {
    Map<String, Object> payloadMap = new LinkedHashMap<>();
    payloadMap.put("label", "Tea");
    payloadMap.put("slug", "tea");
    payloadMap.put("map", "TEA");

    String payload = objectMapper.writeValueAsString(payloadMap);

    mockMvc
        .perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.label").value("Tea"))
        .andExpect(jsonPath("$.slug").value("tea"))
        .andExpect(jsonPath("$.map").value("TEA"))
        .andExpect(jsonPath("$.children").isArray());
  }

  @Test
  void shouldUpdateCategoryUsingContractPayload() throws Exception {
    Category root =
        categoryRepository.save(Category.builder().label("Tea").slug("tea").map("TEA").build());
    Category child =
        categoryRepository.save(
            Category.builder()
                .label("Green Tea")
                .slug("green-tea")
                .map("GREEN")
                .parent(root)
                .build());

    Map<String, Object> payloadMap = new LinkedHashMap<>();
    payloadMap.put("label", "Black Tea");
    payloadMap.put("slug", "black-tea");
    payloadMap.put("map", "BLACK");
    payloadMap.put("parent_id", null);

    String payload = objectMapper.writeValueAsString(payloadMap);

    mockMvc
        .perform(
            put("/categories/{id}", child.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(child.getId()))
        .andExpect(jsonPath("$.label").value("Black Tea"))
        .andExpect(jsonPath("$.slug").value("black-tea"))
        .andExpect(jsonPath("$.parent_id").doesNotExist());
  }

  @Test
  void shouldCreateProductUsingContractPayload() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "category_id", category.getId(),
                "label", "Wild Pepper",
                "reference", "PEP-NEW",
                "limit_date", "2026-12-31",
                "description", "Fresh pepper",
                "size", "kg",
                "is_active", true));

    mockMvc
        .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.category_id").value(category.getId()))
        .andExpect(jsonPath("$.label").value("Wild Pepper"))
        .andExpect(jsonPath("$.reference").value("PEP-NEW"))
        .andExpect(jsonPath("$.is_active").value(true));
  }

  @Test
  void shouldUpdateProductUsingContractPayload() throws Exception {
    Category initialCategory =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    Category updatedCategory =
        categoryRepository.save(Category.builder().label("Tea").slug("tea").map("TEA").build());
    Product product =
        productRepository.save(
            product(
                "Wild Pepper", "PEP-001", true, "Hot spice", initialCategory, "MGA", "kg", 12000));

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "category_id", updatedCategory.getId(),
                "label", "Smoked Tea",
                "reference", "TEA-001",
                "limit_date", "2027-01-31",
                "description", "Updated description",
                "size", "box",
                "is_active", false));

    mockMvc
        .perform(
            put("/products/{id}", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId()))
        .andExpect(jsonPath("$.category_id").value(updatedCategory.getId()))
        .andExpect(jsonPath("$.label").value("Smoked Tea"))
        .andExpect(jsonPath("$.reference").value("TEA-001"))
        .andExpect(jsonPath("$.is_active").value(false));
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
