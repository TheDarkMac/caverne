package com.devikapps.caverne.modules.catalog;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NichesCatalogImporter implements ApplicationRunner {

  private final NichesImportProperties properties;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (!properties.isEnabled()) {
      return;
    }

    Path path = Path.of(properties.getPath());
    if (!Files.exists(path)) {
      throw new IllegalStateException("Niches import file not found: " + path);
    }

    for (NicheCategory nicheCategory : parse(path)) {
      Category category = findOrCreateCategory(nicheCategory.name());
      for (NicheProduct nicheProduct : nicheCategory.products()) {
        findOrCreateProduct(category, nicheProduct);
      }
    }
  }

  private List<NicheCategory> parse(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    Map<String, List<NicheProduct>> categories = new LinkedHashMap<>();
    String currentCategory = null;

    for (String rawLine : lines) {
      String line = rawLine == null ? "" : rawLine.trim();
      if (line.isBlank()) {
        continue;
      }

      if (line.startsWith("#")) {
        currentCategory = normalizeLabel(line.substring(1).replace(":", "").trim());
        categories.computeIfAbsent(currentCategory, ignored -> new ArrayList<>());
        continue;
      }

      if (currentCategory == null) {
        continue;
      }

      String cleaned = line.replaceFirst("^[\\-–—•]+", "").trim();
      if (cleaned.isBlank()) {
        continue;
      }
      categories.get(currentCategory).add(parseProduct(cleaned));
    }

    return categories.entrySet().stream()
        .map(entry -> new NicheCategory(entry.getKey(), entry.getValue()))
        .toList();
  }

  private NicheProduct parseProduct(String rawValue) {
    String[] parts = rawValue.split("\\|");
    String label = normalizeLabel(parts[0]);
    if (parts.length == 1) {
      return new NicheProduct(label, null, null, null);
    }

    BigDecimal amount = new BigDecimal(parts[1].trim());
    String currency = parts.length >= 3 ? normalizeLabel(parts[2]).toUpperCase(Locale.ROOT) : "MGA";
    String unit = parts.length >= 4 ? normalizeLabel(parts[3]) : "unit";
    return new NicheProduct(label, amount, currency, unit);
  }

  private Category findOrCreateCategory(String label) {
    String slug = slugify(label);
    return categoryRepository
        .findBySlugIgnoreCase(slug)
        .orElseGet(
            () ->
                categoryRepository.save(
                    Category.builder()
                        .label(label)
                        .slug(slug)
                        .map(slug.toUpperCase(Locale.ROOT))
                        .build()));
  }

  private Product findOrCreateProduct(Category category, NicheProduct nicheProduct) {
    Product product =
        productRepository
            .findByLabelIgnoreCase(nicheProduct.label())
            .orElseGet(() -> buildProduct(category, nicheProduct));
    if (nicheProduct.amount() != null
        && product.getPrices().stream()
            .noneMatch(
                price ->
                    nicheProduct.currency().equalsIgnoreCase(price.getCurrencyCode())
                        && nicheProduct.unit().equalsIgnoreCase(price.getUnit()))) {
      product.getPrices().addAll(buildBootstrapPriceHistory(product, nicheProduct));
    }
    return productRepository.save(product);
  }

  private Product buildProduct(Category category, NicheProduct nicheProduct) {
    Product product =
        Product.builder()
            .category(category)
            .label(nicheProduct.label())
            .reference(buildReference(category, nicheProduct.label()))
            .description("Imported from niches.md")
            .size("1")
            .stockQuantity(BigDecimal.valueOf(100))
            .isActive(true)
            .limitDate(LocalDate.now().plusYears(2))
            .build();
    if (nicheProduct.amount() != null) {
      product.setPrices(buildBootstrapPriceHistory(product, nicheProduct));
    }
    return product;
  }

  private List<Price> buildBootstrapPriceHistory(Product product, NicheProduct nicheProduct) {
    // Generate one price entry per year from 2020-01-01 up to (but not after) today.
    // Each year applies a 5 % annual increase relative to the imported amount,
    // simulating realistic price evolution. The imported amount is treated as the
    // 2025 reference price; earlier years are back-calculated.
    LocalDate start = LocalDate.of(2020, 1, 1);
    LocalDate today = LocalDate.now();
    int referenceYear = 2025;
    BigDecimal rate = new BigDecimal("1.05"); // 5 % per year

    List<Price> prices = new ArrayList<>();
    LocalDate date = start;
    while (!date.isAfter(today)) {
      int yearDiff = date.getYear() - referenceYear; // negative for past years
      BigDecimal factor = rate.pow(Math.abs(yearDiff)).setScale(6, RoundingMode.HALF_UP);
      BigDecimal amount;
      if (yearDiff >= 0) {
        amount = nicheProduct.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
      } else {
        // divide to go back in time
        amount = nicheProduct.amount().divide(factor, 2, RoundingMode.HALF_UP);
      }
      prices.add(buildBootstrapPrice(product, nicheProduct, amount, date));
      date = date.plusYears(1);
    }
    return prices;
  }

  private Price buildBootstrapPrice(
      Product product, NicheProduct nicheProduct, BigDecimal amount, LocalDate validFrom) {
    return Price.builder()
        .product(product)
        .currencyCode(nicheProduct.currency())
        .value(amount)
        .validFrom(validFrom)
        .unit(nicheProduct.unit())
        .build();
  }

  private String buildReference(Category category, String productLabel) {
    String base =
        (slugify(category.getSlug() == null ? category.getLabel() : category.getSlug())
                + "-"
                + slugify(productLabel))
            .toUpperCase(Locale.ROOT);
    String reference = base.length() <= 100 ? base : base.substring(0, 100);
    int suffix = 1;
    while (productRepository.existsByReferenceIgnoreCase(reference)) {
      String candidate = base;
      String suffixText = "-" + suffix++;
      int maxBaseLength = Math.max(1, 100 - suffixText.length());
      if (candidate.length() > maxBaseLength) {
        candidate = candidate.substring(0, maxBaseLength);
      }
      reference = candidate + suffixText;
    }
    return reference;
  }

  private String normalizeLabel(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }

  private String slugify(String value) {
    String normalized =
        Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
    String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    return slug.isBlank() ? "catalog-item" : slug;
  }

  private record NicheCategory(String name, List<NicheProduct> products) {}

  private record NicheProduct(String label, BigDecimal amount, String currency, String unit) {}
}
