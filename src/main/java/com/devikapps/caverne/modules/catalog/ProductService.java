package com.devikapps.caverne.modules.catalog;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

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

  public Page<org.openapitools.client.model.Product> findAll(
      Long categoryId, Boolean isActive, String search, String currency, Pageable pageable) {
    Page<Product> page =
        productRepository.findAll(
            Specification.where(withCategory(categoryId))
                .and(withActiveState(isActive))
                .and(withSearch(search))
                .and(withCurrency(currency)),
            pageable);

    return page.map(product -> productApiMapper.toResponse(product, currency));
  }

  @Transactional
  public UpsertProductResult createProduct(org.openapitools.client.model.ProductInput input) {
    if (input.getId() != null) {
      Product existing = productRepository.findById(input.getId().longValue()).orElse(null);
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
      Long id, org.openapitools.client.model.ProductInput input) {
    if (input.getId() != null && !id.equals(input.getId().longValue())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "Product payload id does not match path id");
    }

    Product existing = productRepository.findById(id).orElse(null);
    Product product = productApiMapper.fromInput(input, existing);
    return new UpsertProductResult(
        productApiMapper.toResponse(productRepository.save(product), null), existing == null);
  }

  public Product findById(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found"));
  }

  public org.openapitools.client.model.Product findResponseById(Long id) {
    return productApiMapper.toResponse(findById(id), null);
  }

  @Transactional
  public void deleteProduct(Long id) {
    productRepository.deleteById(id);
  }

  private Specification<Product> withCategory(Long categoryId) {
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
