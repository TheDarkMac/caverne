package com.devikapps.caverne.modules.catalog;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;
  private final SecurityActorResolver securityActorResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listProducts(
      @RequestParam(required = false) UUID category_id,
      @RequestParam(required = false) Boolean is_active,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String currency,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {

    var p =
        productService.findAll(
            category_id, is_active, search, currency, PageRequest.of(page - 1, per_page));
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
  public String getProduct(@PathVariable UUID id) {
    return JSON.getGson().toJson(productService.findResponseById(id));
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
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Invalid product payload");
    }
  }

  private org.openapitools.client.model.ProductStockInput parseProductStockInput(String rawBody) {
    try {
      return org.openapitools.client.model.ProductStockInput.fromJson(rawBody);
    } catch (Exception exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Invalid product stock payload");
    }
  }
}
