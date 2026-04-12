package com.devikapps.caverne.modules.catalog;

import java.math.BigDecimal;
import java.net.URI;
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
                    .icon(product.getCategory().getIcon())
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
        .images(
            product.getImages().stream()
                .map(
                    image ->
                        new org.openapitools.client.model.ProductImage()
                            .id(image.getId())
                            .productId(product.getId())
                            .url(parseUri(image.getUrl()))
                            .isMain(image.isMain()))
                .toList())
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

    product.getImages().clear();
    if (input.getImages() != null) {
      boolean hasMainImage =
          input.getImages().stream().anyMatch(image -> Boolean.TRUE.equals(image.getIsMain()));
      for (int i = 0; i < input.getImages().size(); i++) {
        org.openapitools.client.model.ProductImageInput imageInput = input.getImages().get(i);
        if (imageInput.getUrl() == null) {
          continue;
        }
        product
            .getImages()
            .add(
                ProductImage.builder()
                    .id(imageInput.getId())
                    .product(product)
                    .url(imageInput.getUrl().toString())
                    .isMain(
                        Boolean.TRUE.equals(imageInput.getIsMain())
                            || (!hasMainImage && i == 0))
                    .build());
      }
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

  private URI parseUri(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return URI.create(value);
  }
}
