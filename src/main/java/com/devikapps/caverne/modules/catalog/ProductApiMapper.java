package com.devikapps.caverne.modules.catalog;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductApiMapper {

  public org.openapitools.client.model.Product toResponse(Product product, String currency) {
    List<org.openapitools.client.model.Price> prices =
        selectCurrentPrices(product.getPrices(), currency).stream()
            .map(price -> toPriceModel(product, price))
            .toList();

    return baseProductModel(product).prices(prices);
  }

  public org.openapitools.client.model.Product toOrderSnapshot(Product product, Price price) {
    return baseProductModel(product).prices(List.of(toPriceModel(product, price)));
  }

  private org.openapitools.client.model.Product baseProductModel(Product product) {
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
        .stockQuantity(
            product.getStockQuantity() == null ? null : product.getStockQuantity().doubleValue())
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
                .toList());
  }

  public Product fromInput(org.openapitools.client.model.ProductInput input, Product existing) {
    Product product = existing == null ? new Product() : existing;
    product.setLabel(input.getLabel());
    product.setReference(input.getReference());
    product.setLimitDate(input.getLimitDate());
    product.setDescription(input.getDescription());
    product.setSize(input.getSize() == null ? null : input.getSize().toPlainString());
    product.setStockQuantity(
        input.getStockQuantity() == null
            ? existing == null ? BigDecimal.ZERO : product.getStockQuantity()
            : BigDecimal.valueOf(input.getStockQuantity()));
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
                        Boolean.TRUE.equals(imageInput.getIsMain()) || (!hasMainImage && i == 0))
                    .build());
      }
    }

    if (input.getPrices() != null) {
      product.getPrices().clear();
      for (org.openapitools.client.model.PriceInput priceInput : input.getPrices()) {
        if (priceInput.getCurrencyCode() == null
            || priceInput.getValue() == null
            || priceInput.getValidFrom() == null
            || priceInput.getUnit() == null) {
          continue;
        }
        product
            .getPrices()
            .add(
                Price.builder()
                    .id(priceInput.getId())
                    .product(product)
                    .currencyCode(priceInput.getCurrencyCode())
                    .value(BigDecimal.valueOf(priceInput.getValue()))
                    .validFrom(priceInput.getValidFrom())
                    .unit(priceInput.getUnit())
                    .build());
      }
    }

    return product;
  }

  private List<Price> selectCurrentPrices(List<Price> prices, String currency) {
    LocalDate today = LocalDate.now();
    Map<String, Price> currentByKey =
        prices.stream()
            .filter(
                price ->
                    currency == null
                        || currency.isBlank()
                        || currency.equalsIgnoreCase(price.getCurrencyCode()))
            .collect(
                Collectors.toMap(
                    price -> currentPriceKey(price.getCurrencyCode(), price.getUnit()),
                    Function.identity(),
                    (left, right) -> newerApplicablePrice(left, right, today)));

    return currentByKey.values().stream()
        .sorted(
            Comparator.comparing(Price::getCurrencyCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(Price::getUnit, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  private Price newerApplicablePrice(Price left, Price right, LocalDate today) {
    return comparePriceRecency(left, right, today) >= 0 ? left : right;
  }

  private int comparePriceRecency(Price left, Price right, LocalDate today) {
    LocalDate leftDate = normalizeApplicableDate(left.getValidFrom(), today);
    LocalDate rightDate = normalizeApplicableDate(right.getValidFrom(), today);
    return leftDate.compareTo(rightDate);
  }

  private LocalDate normalizeApplicableDate(LocalDate validFrom, LocalDate today) {
    if (validFrom == null) {
      return LocalDate.MIN;
    }
    return validFrom.isAfter(today) ? LocalDate.MIN : validFrom;
  }

  private String currentPriceKey(String currencyCode, String unit) {
    return (currencyCode == null ? "" : currencyCode.toUpperCase())
        + "::"
        + Objects.toString(unit, "");
  }

  private org.openapitools.client.model.Price toPriceModel(Product product, Price price) {
    return new org.openapitools.client.model.Price()
        .id(price.getId())
        .productId(product.getId())
        .currencyCode(price.getCurrencyCode())
        .value(price.getValue().doubleValue())
        .validFrom(price.getValidFrom())
        .unit(price.getUnit());
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
