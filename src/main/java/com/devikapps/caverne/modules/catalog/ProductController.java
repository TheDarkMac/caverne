package com.devikapps.caverne.modules.catalog;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;
  private final SecurityActorResolver securityActorResolver;
  private final StockMovementService stockMovementService;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listProducts(
      @RequestParam(required = false) UUID category_id,
      @RequestParam(required = false) Boolean is_active,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String currency,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_to,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {

    var p =
        productService.findAll(
            category_id,
            is_active,
            search,
            currency,
            price_date,
            price_from,
            price_to,
            PageRequest.of(page - 1, per_page));
    org.openapitools.client.model.ProductsGet200Response response =
        new org.openapitools.client.model.ProductsGet200Response()
            .data(p.getContent())
            .meta(
                new org.openapitools.client.model.PaginatedMeta()
                    .total(Math.toIntExact(p.getTotalElements()))
                    .page(p.getNumber() + 1)
                    .perPage(p.getSize())
                    .lastPage(p.getTotalPages()));
    return JSON.getGson().toJson(response);
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> createProduct(
      @RequestBody String rawBody) {
    ProductService.UpsertProductResult result =
        productService.createProduct(parseProductInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.product()));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getProduct(
      @PathVariable UUID id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate price_to) {
    return JSON.getGson()
        .toJson(productService.findResponseById(id, price_date, price_from, price_to));
  }

  @GetMapping(value = "/{id}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getProductStock(@PathVariable UUID id) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(productService.findStockById(id));
  }

  @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> updateProduct(
      @PathVariable UUID id, @RequestBody String rawBody) {
    ProductService.UpsertProductResult result =
        productService.updateProduct(id, parseProductInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.product()));
  }

  @GetMapping(value = "/{id}/stock/movements", produces = MediaType.APPLICATION_JSON_VALUE)
  public java.util.Map<String, Object> listStockMovements(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {
    securityActorResolver.requireAdmin();
    org.springframework.data.domain.Page<StockMovement> p =
        stockMovementService.listByProduct(id, PageRequest.of(page - 1, per_page));
    java.util.List<java.util.Map<String, Object>> data =
        p.getContent().stream().map(ProductController::stockMovementToMap).toList();
    java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
    meta.put("total", Math.toIntExact(p.getTotalElements()));
    meta.put("page", p.getNumber() + 1);
    meta.put("per_page", p.getSize());
    meta.put("last_page", p.getTotalPages());
    java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
    out.put("data", data);
    out.put("meta", meta);
    return out;
  }

  private static java.util.Map<String, Object> stockMovementToMap(StockMovement movement) {
    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
    map.put("id", movement.getId());
    map.put("product_id", movement.getProductId());
    map.put("delta", movement.getDelta());
    map.put("reason", movement.getReason() == null ? null : movement.getReason().name());
    map.put("balance_after", movement.getBalanceAfter());
    map.put(
        "created_at", movement.getCreatedAt() == null ? null : movement.getCreatedAt().toString());
    map.put("created_by", movement.getCreatedBy());
    map.put("note", movement.getNote());
    return map;
  }

  @PutMapping(value = "/{id}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateProductStock(@PathVariable UUID id, @RequestBody String rawBody) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(productService.updateStock(id, parseProductStockInput(rawBody)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProduct(@PathVariable UUID id) {
    productService.deleteProduct(id);
  }

  private org.openapitools.client.model.ProductInput parseProductInput(String rawBody) {
    try {
      return org.openapitools.client.model.ProductInput.fromJson(rawBody);
    } catch (Exception exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT, "Invalid product payload");
    }
  }

  private org.openapitools.client.model.ProductStockInput parseProductStockInput(String rawBody) {
    try {
      return org.openapitools.client.model.ProductStockInput.fromJson(rawBody);
    } catch (Exception exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT,
          "Invalid product stock payload");
    }
  }
}
