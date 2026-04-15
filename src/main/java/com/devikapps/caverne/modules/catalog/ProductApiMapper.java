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
    return toResponse(product, currency, null, null, null);
  }

  /**
   * Returns the product with its effective prices.
   *
   * <p>When {@code priceFrom} or {@code priceTo} are provided, all price records within that range
   * are returned (history view). Otherwise, the single most-recent price per (currency, unit) pair
   * that is not after {@code asOf} (defaults to today) is returned.
   */
  public org.openapitools.client.model.Product toResponse(
      Product product, String currency, LocalDate asOf, LocalDate priceFrom, LocalDate priceTo) {
    List<org.openapitools.client.model.Price> prices;
    if (priceFrom != null || priceTo != null) {
      prices =
          selectPricesInRange(product.getPrices(), currency, priceFrom, priceTo).stream()
              .map(price -> toPriceModel(product, price))
              .toList();
    } else {
      prices =
          selectCurrentPrices(product.getPrices(), currency, asOf != null ? asOf : LocalDate.now())
              .stream()
              .map(price -> toPriceModel(product, price))
              .toList();
    }
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
                            .main(image.isMain()))
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
          input.getImages().stream().anyMatch(image -> Boolean.TRUE.equals(image.getMain()));
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
                    .isMain(Boolean.TRUE.equals(imageInput.getMain()) || (!hasMainImage && i == 0))
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

  private List<Price> selectCurrentPrices(List<Price> prices, String currency, LocalDate asOf) {
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
                    (left, right) -> newerApplicablePrice(left, right, asOf)));

    return currentByKey.values().stream()
        .sorted(
            Comparator.comparing(Price::getCurrencyCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(Price::getUnit, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  private List<Price> selectPricesInRange(
      List<Price> prices, String currency, LocalDate from, LocalDate to) {
    return prices.stream()
        .filter(
            price ->
                currency == null
                    || currency.isBlank()
                    || currency.equalsIgnoreCase(price.getCurrencyCode()))
        .filter(
            price -> {
              LocalDate d = price.getValidFrom();
              if (d == null) return false;
              if (from != null && d.isBefore(from)) return false;
              if (to != null && d.isAfter(to)) return false;
              return true;
            })
        .sorted(
            Comparator.comparing(Price::getValidFrom, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparing(Price::getCurrencyCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(Price::getUnit, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  private Price newerApplicablePrice(Price left, Price right, LocalDate asOf) {
    return comparePriceRecency(left, right, asOf) >= 0 ? left : right;
  }

  private int comparePriceRecency(Price left, Price right, LocalDate asOf) {
    LocalDate leftDate = normalizeApplicableDate(left.getValidFrom(), asOf);
    LocalDate rightDate = normalizeApplicableDate(right.getValidFrom(), asOf);
    return leftDate.compareTo(rightDate);
  }

  private LocalDate normalizeApplicableDate(LocalDate validFrom, LocalDate asOf) {
    if (validFrom == null) {
      return LocalDate.MIN;
    }
    return validFrom.isAfter(asOf) ? LocalDate.MIN : validFrom;
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
