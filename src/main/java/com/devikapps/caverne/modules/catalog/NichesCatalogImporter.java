package com.devikapps.caverne.modules.catalog;

import java.io.IOException;
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
      for (String productLabel : nicheCategory.products()) {
        findOrCreateProduct(category, productLabel);
      }
    }
  }

  private List<NicheCategory> parse(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    Map<String, List<String>> categories = new LinkedHashMap<>();
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
      categories.get(currentCategory).add(normalizeLabel(cleaned));
    }

    return categories.entrySet().stream()
        .map(entry -> new NicheCategory(entry.getKey(), entry.getValue()))
        .toList();
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

  private Product findOrCreateProduct(Category category, String label) {
    return productRepository
        .findByCategoryIdAndLabelIgnoreCase(category.getId(), label)
        .orElseGet(() -> productRepository.save(buildProduct(category, label)));
  }

  private Product buildProduct(Category category, String label) {
    return Product.builder()
        .category(category)
        .label(label)
        .reference(buildReference(category, label))
        .description("Imported from niches.md")
        .size("1")
        .isActive(true)
        .limitDate(LocalDate.now().plusYears(2))
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

  private record NicheCategory(String name, List<String> products) {}
}
