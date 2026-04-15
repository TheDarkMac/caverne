package com.devikapps.caverne.modules.catalog;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import com.devikapps.caverne.modules.user.UserAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductApiMapper productApiMapper;
  private final StockMovementService stockMovementService;
  private final SecurityActorResolver securityActorResolver;

  public Page<org.openapitools.client.model.Product> findAll(
      UUID categoryId,
      Boolean isActive,
      String search,
      String currency,
      LocalDate priceDate,
      LocalDate priceFrom,
      LocalDate priceTo,
      Pageable pageable) {
    Page<Product> page =
        productRepository.findAll(
            Specification.where(withCategory(categoryId))
                .and(withActiveState(isActive))
                .and(withSearch(search))
                .and(withCurrency(currency)),
            pageable);

    return page.map(
        product -> productApiMapper.toResponse(product, currency, priceDate, priceFrom, priceTo));
  }

  @Transactional
  public UpsertProductResult createProduct(org.openapitools.client.model.ProductInput input) {
    if (input.getId() != null) {
      Product existing = productRepository.findById(input.getId()).orElse(null);
      Product product = productApiMapper.fromInput(input, existing);
      return new UpsertProductResult(
          productApiMapper.toResponse(productRepository.save(product), null), existing == null);
    }

    Product product = productApiMapper.fromInput(input, null);
    return new UpsertProductResult(
        productApiMapper.toResponse(productRepository.save(product), null), true);
  }

  @Transactional
  public UpsertProductResult updateProduct(
      UUID id, org.openapitools.client.model.ProductInput input) {
    if (input.getId() != null && !id.equals(input.getId())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "Product payload id does not match path id");
    }

    Product existing = productRepository.findById(id).orElse(null);
    Product product = productApiMapper.fromInput(input, existing);
    return new UpsertProductResult(
        productApiMapper.toResponse(productRepository.save(product), null), existing == null);
  }

  public Product findById(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found"));
  }

  public org.openapitools.client.model.Product findResponseById(UUID id) {
    return productApiMapper.toResponse(findById(id), null, null, null, null);
  }

  public org.openapitools.client.model.Product findResponseById(
      UUID id, LocalDate priceDate, LocalDate priceFrom, LocalDate priceTo) {
    return productApiMapper.toResponse(findById(id), null, priceDate, priceFrom, priceTo);
  }

  public org.openapitools.client.model.ProductStock findStockById(UUID id) {
    Product product = findById(id);
    return new org.openapitools.client.model.ProductStock()
        .productId(product.getId())
        .quantity(product.getStockQuantity().doubleValue());
  }

  @Transactional
  public org.openapitools.client.model.ProductStock updateStock(
      UUID id, org.openapitools.client.model.ProductStockInput input) {
    if (input.getQuantity() == null) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Stock quantity is required");
    }
    if (input.getQuantity() < 0) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Stock quantity cannot be negative");
    }

    Product product = findById(id);
    BigDecimal previous =
        product.getStockQuantity() == null ? BigDecimal.ZERO : product.getStockQuantity();
    BigDecimal newBalance = BigDecimal.valueOf(input.getQuantity());
    product.setStockQuantity(newBalance);
    Product saved = productRepository.save(product);

    UserAccount actor = securityActorResolver.resolveUserOrNull();
    stockMovementService.record(
        saved.getId(),
        newBalance,
        newBalance.subtract(previous),
        StockMovement.Reason.MANUAL_ADJUSTMENT,
        actor == null ? null : actor.getId(),
        null);

    return new org.openapitools.client.model.ProductStock()
        .productId(saved.getId())
        .quantity(saved.getStockQuantity().doubleValue());
  }

  @Transactional
  public void deleteProduct(UUID id) {
    productRepository.deleteById(id);
  }

  private Specification<Product> withCategory(UUID categoryId) {
    return (root, query, builder) ->
        categoryId == null ? null : builder.equal(root.get("category").get("id"), categoryId);
  }

  private Specification<Product> withActiveState(Boolean isActive) {
    return (root, query, builder) ->
        isActive == null ? null : builder.equal(root.get("isActive"), isActive);
  }

  private Specification<Product> withSearch(String search) {
    return (root, query, builder) -> {
      if (search == null || search.isBlank()) {
        return null;
      }

      String pattern = "%" + search.toLowerCase() + "%";
      return builder.or(
          builder.like(builder.lower(root.get("label")), pattern),
          builder.like(builder.lower(root.get("description")), pattern),
          builder.like(builder.lower(root.get("reference")), pattern));
    };
  }

  private Specification<Product> withCurrency(String currency) {
    return (root, query, builder) -> {
      if (currency == null || currency.isBlank()) {
        return null;
      }

      query.distinct(true);
      return builder.equal(root.join("prices").get("currencyCode"), currency);
    };
  }

  public record UpsertProductResult(
      org.openapitools.client.model.Product product, boolean created) {}
}
