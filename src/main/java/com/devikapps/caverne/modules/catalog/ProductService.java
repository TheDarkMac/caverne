package com.devikapps.caverne.modules.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Page<org.openapitools.client.model.Product> findAll(
            Long categoryId,
            Boolean isActive,
            String search,
            String currency,
            Pageable pageable
    ) {
        Page<Product> page = productRepository.findAll(
                Specification.where(withCategory(categoryId))
                        .and(withActiveState(isActive))
                        .and(withSearch(search))
                        .and(withCurrency(currency)),
                pageable
        );

        return page.map(product -> toResponse(product, currency));
    }

    @Transactional
    public org.openapitools.client.model.Product createProduct(org.openapitools.client.model.ProductInput input) {
        Product product = fromInput(input, null);
        return toResponse(productRepository.save(product), null);
    }

    @Transactional
    public org.openapitools.client.model.Product updateProduct(Long id, org.openapitools.client.model.ProductInput input) {
        Product existing = findById(id);
        Product product = fromInput(input, existing);
        return toResponse(productRepository.save(product), null);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found"));
    }

    public org.openapitools.client.model.Product findResponseById(Long id) {
        return toResponse(findById(id), null);
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
                    builder.like(builder.lower(root.get("reference")), pattern)
            );
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

    private org.openapitools.client.model.Product toResponse(Product product, String currency) {
        List<org.openapitools.client.model.Price> prices = product.getPrices().stream()
                .filter(price -> currency == null || currency.isBlank() || currency.equalsIgnoreCase(price.getCurrencyCode()))
                .sorted(Comparator.comparing(Price::getValidFrom).reversed())
                .map(price -> new org.openapitools.client.model.Price()
                        .id(price.getId().intValue())
                        .productId(product.getId().intValue())
                        .currencyCode(price.getCurrencyCode())
                        .value(price.getValue().doubleValue())
                        .validFrom(price.getValidFrom())
                        .unit(price.getUnit()))
                .toList();

        return new org.openapitools.client.model.Product()
                .id(product.getId().intValue())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId().intValue())
                .label(product.getLabel())
                .reference(product.getReference())
                .limitDate(product.getLimitDate())
                .description(product.getDescription())
                .size(product.getSize())
                .isActive(product.isActive())
                .prices(prices);
    }

    private Product fromInput(org.openapitools.client.model.ProductInput input, Product existing) {
        Product product = existing == null ? new Product() : existing;
        product.setLabel(input.getLabel());
        product.setReference(input.getReference());
        product.setLimitDate(input.getLimitDate());
        product.setDescription(input.getDescription());
        product.setSize(input.getSize());
        product.setActive(Boolean.TRUE.equals(input.getIsActive()));

        if (input.getCategoryId() != null) {
            Category category = new Category();
            category.setId(input.getCategoryId().longValue());
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return product;
    }
}
