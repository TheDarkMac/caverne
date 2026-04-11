package com.devikapps.caverne.modules.catalog;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductApiMapper {

  public org.openapitools.client.model.Product toResponse(Product product, String currency) {
    List<org.openapitools.client.model.Price> prices =
        product.getPrices().stream()
            .filter(
                price ->
                    currency == null
                        || currency.isBlank()
                        || currency.equalsIgnoreCase(price.getCurrencyCode()))
            .sorted(Comparator.comparing(Price::getValidFrom).reversed())
            .map(
                price ->
                    new org.openapitools.client.model.Price()
                        .id(price.getId())
                        .productId(product.getId())
                        .currencyCode(price.getCurrencyCode())
                        .value(price.getValue().doubleValue())
                        .validFrom(price.getValidFrom())
                        .unit(price.getUnit()))
            .toList();

    return new org.openapitools.client.model.Product()
        .id(product.getId())
        .category(
            product.getCategory() == null
                ? null
                : new org.openapitools.client.model.Category()
                    .id(product.getCategory().getId())
                    .label(product.getCategory().getLabel())
                    .slug(product.getCategory().getSlug())
                    .map(product.getCategory().getMap())
                    .parentId(
                        product.getCategory().getParent() == null
                            ? null
                            : product.getCategory().getParent().getId())
                    .children(java.util.List.of()))
        .label(product.getLabel())
        .reference(product.getReference())
        .limitDate(product.getLimitDate())
        .description(product.getDescription())
        .size(parseSize(product.getSize()))
        .isActive(product.isActive())
        .prices(prices);
  }

  public Product fromInput(org.openapitools.client.model.ProductInput input, Product existing) {
    Product product = existing == null ? new Product() : existing;
    product.setLabel(input.getLabel());
    product.setReference(input.getReference());
    product.setLimitDate(input.getLimitDate());
    product.setDescription(input.getDescription());
    product.setSize(input.getSize() == null ? null : input.getSize().toPlainString());
    product.setActive(Boolean.TRUE.equals(input.getIsActive()));

    if (input.getCategory() != null && input.getCategory().getId() != null) {
      Category category = new Category();
      category.setId(input.getCategory().getId());
      product.setCategory(category);
    } else {
      product.setCategory(null);
    }

    return product;
  }

  private BigDecimal parseSize(String size) {
    if (size == null || size.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(size);
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
