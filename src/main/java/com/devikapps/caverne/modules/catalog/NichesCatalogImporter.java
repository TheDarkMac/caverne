package com.devikapps.caverne.modules.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    for (String categoryName : parse(path)) {
      findOrCreateCategory(categoryName);
    }
  }

  private List<String> parse(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    Set<String> categories = new LinkedHashSet<>();

    for (String rawLine : lines) {
      String line = rawLine == null ? "" : rawLine.trim();
      if (line.isBlank() || !line.startsWith("#")) {
        continue;
      }
      String name = normalizeLabel(line.substring(1).replace(":", "").trim());
      if (!name.isBlank()) {
        categories.add(name);
      }
    }

    return new ArrayList<>(categories);
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
}
