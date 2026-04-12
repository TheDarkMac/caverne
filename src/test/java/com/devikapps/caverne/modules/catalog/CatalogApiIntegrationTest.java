package com.devikapps.caverne.modules.catalog;

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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private AuthSessionRepository authSessionRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authSessionRepository.deleteAll();
    userRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void shouldListCategoriesAsTree() throws Exception {
    Category root =
        categoryRepository.save(
            Category.builder()
                .label("Tea")
                .slug("tea")
                .icon("https://cdn.caverne.test/icons/tea.svg")
                .map("TEA")
                .build());

    categoryRepository.save(
        Category.builder().label("Green Tea").slug("green-tea").map("GREEN").parent(root).build());

    mockMvc
        .perform(get("/categories").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Tea"))
        .andExpect(jsonPath("$[0].icon").value("https://cdn.caverne.test/icons/tea.svg"))
        .andExpect(jsonPath("$[0].children[0].label").value("Green Tea"))
        .andExpect(jsonPath("$[0].children[0].parent_id").value(root.getId().toString()));
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
        .andExpect(jsonPath("$.data[0].category.id").value(category.getId().toString()))
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
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.label").value("Forest Honey"))
        .andExpect(jsonPath("$.reference").value("HON-001"))
        .andExpect(jsonPath("$.images[0].url").value("https://cdn.caverne.test/products/HON-001.jpg"))
        .andExpect(jsonPath("$.prices[0].value").value(18000));
  }

  @Test
  void shouldCreateCategoryUsingContractPayload() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-category-create@example.com", "secret123");

    Map<String, Object> payloadMap = new LinkedHashMap<>();
    payloadMap.put("label", "Tea");
    payloadMap.put("slug", "tea");
    payloadMap.put("icon", "https://cdn.caverne.test/icons/tea.svg");
    payloadMap.put("map", "TEA");

    String payload = objectMapper.writeValueAsString(payloadMap);

    mockMvc
        .perform(
            post("/categories")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.label").value("Tea"))
        .andExpect(jsonPath("$.slug").value("tea"))
        .andExpect(jsonPath("$.icon").value("https://cdn.caverne.test/icons/tea.svg"))
        .andExpect(jsonPath("$.map").value("TEA"))
        .andExpect(jsonPath("$.children").isArray());
  }

  @Test
  void shouldUpdateCategoryUsingContractPayload() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-category-update@example.com", "secret123");

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
    payloadMap.put("icon", "https://cdn.caverne.test/icons/black-tea.svg");
    payloadMap.put("map", "BLACK");
    payloadMap.put("parent_id", null);

    String payload = objectMapper.writeValueAsString(payloadMap);

    mockMvc
        .perform(
            put("/categories/{id}", child.getId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(child.getId().toString()))
        .andExpect(jsonPath("$.label").value("Black Tea"))
        .andExpect(jsonPath("$.slug").value("black-tea"))
        .andExpect(jsonPath("$.icon").value("https://cdn.caverne.test/icons/black-tea.svg"))
        .andExpect(jsonPath("$.parent_id").doesNotExist());
  }

  @Test
  void shouldUpdateCategoryThroughPostWhenPayloadContainsId() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-category-post@example.com", "secret123");

    Category category =
        categoryRepository.save(Category.builder().label("Tea").slug("tea").map("TEA").build());

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "id",
                category.getId(),
                "label",
                "Herbal Tea",
                "slug",
                "herbal-tea",
                "icon",
                "https://cdn.caverne.test/icons/herbal-tea.svg",
                "map",
                "HERBAL"));

    mockMvc
        .perform(
            post("/categories")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(category.getId().toString()))
        .andExpect(jsonPath("$.label").value("Herbal Tea"))
        .andExpect(jsonPath("$.icon").value("https://cdn.caverne.test/icons/herbal-tea.svg"))
        .andExpect(jsonPath("$.slug").value("herbal-tea"));
  }

  @Test
  void shouldCreateCategoryThroughPutWhenIdDoesNotExist() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-category-put@example.com", "secret123");

    UUID newCategoryId = UUID.randomUUID();
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "id",
                newCategoryId,
                "label",
                "Coffee",
                "slug",
                "coffee",
                "icon",
                "https://cdn.caverne.test/icons/coffee.svg",
                "map",
                "COFFEE"));

    mockMvc
        .perform(
            put("/categories/{id}", newCategoryId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.label").value("Coffee"))
        .andExpect(jsonPath("$.icon").value("https://cdn.caverne.test/icons/coffee.svg"))
        .andExpect(jsonPath("$.slug").value("coffee"));
  }

  @Test
  void shouldCreateProductUsingContractPayload() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-product-create@example.com", "secret123");

    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "category", Map.of("id", category.getId()),
                "label", "Wild Pepper",
                "reference", "PEP-NEW",
                "limit_date", "2026-12-31",
                "description", "Fresh pepper",
                "size", 1,
                "is_active", true,
                "images", List.of(Map.of("url", "https://cdn.caverne.test/products/pepper-main.jpg", "is_main", true))));

    mockMvc
        .perform(
            post("/products")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.category.id").value(category.getId().toString()))
        .andExpect(jsonPath("$.label").value("Wild Pepper"))
        .andExpect(jsonPath("$.reference").value("PEP-NEW"))
        .andExpect(jsonPath("$.images[0].url").value("https://cdn.caverne.test/products/pepper-main.jpg"))
        .andExpect(jsonPath("$.images[0].is_main").value(true))
        .andExpect(jsonPath("$.is_active").value(true));
  }

  @Test
  void shouldUpdateProductUsingContractPayload() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-product-update@example.com", "secret123");

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
                "category", Map.of("id", updatedCategory.getId()),
                "label", "Smoked Tea",
                "reference", "TEA-001",
                "limit_date", "2027-01-31",
                "description", "Updated description",
                "size", 2,
                "is_active", false,
                "images", List.of(Map.of("url", "https://cdn.caverne.test/products/tea-main.jpg", "is_main", true))));

    mockMvc
        .perform(
            put("/products/{id}", product.getId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.category.id").value(updatedCategory.getId().toString()))
        .andExpect(jsonPath("$.label").value("Smoked Tea"))
        .andExpect(jsonPath("$.reference").value("TEA-001"))
        .andExpect(jsonPath("$.images[0].url").value("https://cdn.caverne.test/products/tea-main.jpg"))
        .andExpect(jsonPath("$.is_active").value(false));
  }

  @Test
  void shouldUpdateProductThroughPostWhenPayloadContainsId() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-product-post@example.com", "secret123");

    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    Product product =
        productRepository.save(
            product("Wild Pepper", "PEP-001", true, "Hot spice", category, "MGA", "kg", 12000));

    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "id",
                product.getId(),
                "category",
                Map.of("id", category.getId()),
                "label",
                "Updated Pepper",
                "reference",
                "PEP-002",
                "limit_date",
                "2026-12-31",
                "description",
                "Updated",
                "size",
                1,
                "is_active",
                true,
                "images",
                List.of(Map.of("url", "https://cdn.caverne.test/products/updated-pepper.jpg", "is_main", true))));

    mockMvc
        .perform(
            post("/products")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(product.getId().toString()))
        .andExpect(jsonPath("$.label").value("Updated Pepper"))
        .andExpect(jsonPath("$.images[0].url").value("https://cdn.caverne.test/products/updated-pepper.jpg"))
        .andExpect(jsonPath("$.reference").value("PEP-002"));
  }

  @Test
  void shouldCreateProductThroughPutWhenIdDoesNotExist() throws Exception {
    String adminToken = loginAsAdmin("catalog-admin-product-put@example.com", "secret123");

    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    UUID newProductId = UUID.randomUUID();
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "id",
                newProductId,
                "category",
                Map.of("id", category.getId()),
                "label",
                "New Product",
                "reference",
                "NEW-001",
                "limit_date",
                "2027-01-31",
                "description",
                "Created by put",
                "size",
                2,
                "is_active",
                true,
                "images",
                List.of(Map.of("url", "https://cdn.caverne.test/products/new-product.jpg", "is_main", true))));

    mockMvc
        .perform(
            put("/products/{id}", newProductId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.label").value("New Product"))
        .andExpect(jsonPath("$.images[0].url").value("https://cdn.caverne.test/products/new-product.jpg"))
        .andExpect(jsonPath("$.reference").value("NEW-001"));
  }

  @Test
  void shouldRequireAuthenticationForCatalogWrites() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());

    String categoryPayload =
        objectMapper.writeValueAsString(Map.of("label", "Tea", "slug", "tea", "map", "TEA"));
    String productPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "category", Map.of("id", category.getId()),
                "label", "Wild Pepper",
                "reference", "PEP-NEW",
                "limit_date", "2026-12-31",
                "description", "Fresh pepper",
                "size", 1,
                "is_active", true));

    mockMvc
        .perform(
            post("/categories").contentType(MediaType.APPLICATION_JSON).content(categoryPayload))
        .andExpect(status().isUnauthorized());

    UUID categoryId = UUID.randomUUID();
    mockMvc
        .perform(
            put("/categories/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryPayload))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(productPayload))
        .andExpect(status().isUnauthorized());

    UUID productId = UUID.randomUUID();
    mockMvc
        .perform(
            put("/products/{id}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productPayload))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectNonAdminCatalogWrites() throws Exception {
    Category category =
        categoryRepository.save(
            Category.builder().label("Spices").slug("spices").map("SPICES").build());
    String userToken = loginAsSimpleUser("catalog-user@example.com", "secret123");

    String categoryPayload =
        objectMapper.writeValueAsString(Map.of("label", "Tea", "slug", "tea", "map", "TEA"));
    String productPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "category", Map.of("id", category.getId()),
                "label", "Wild Pepper",
                "reference", "PEP-NEW",
                "limit_date", "2026-12-31",
                "description", "Fresh pepper",
                "size", 1,
                "is_active", true));

    mockMvc
        .perform(
            post("/categories")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryPayload))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/products")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productPayload))
        .andExpect(status().isForbidden());
  }

  private String loginAsAdmin(String email, String password) throws Exception {
    createAdmin(email, password);
    return login(email, password);
  }

  private String loginAsSimpleUser(String email, String password) throws Exception {
    createSimpleUser(email, password);
    return login(email, password);
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

  private UserAccount createAdmin(String email, String password) {
    return userRepository.save(
        UserAccount.builder()
            .firstname("Catalog")
            .lastname("Admin")
            .email(email)
            .phone("+26134" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.ADMIN)
            .status("active")
            .build());
  }

  private UserAccount createSimpleUser(String email, String password) {
    return userRepository.save(
        UserAccount.builder()
            .firstname("Catalog")
            .lastname("User")
            .email(email)
            .phone("+26133" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.SIMPLE_USER)
            .status("active")
            .build());
  }

  private String bearer(String token) {
    return "Bearer " + token;
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

    ProductImage image =
        ProductImage.builder()
            .product(product)
            .url("https://cdn.caverne.test/products/" + reference + ".jpg")
            .isMain(true)
            .build();

    product.setImages(List.of(image));
    product.setPrices(List.of(price));
    return product;
  }
}
