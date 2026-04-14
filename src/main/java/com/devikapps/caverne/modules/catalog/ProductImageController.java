package com.devikapps.caverne.modules.catalog;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products/{productId}/images")
@RequiredArgsConstructor
public class ProductImageController {

  private final ProductImageService productImageService;
  private final SecurityActorResolver securityActorResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Map<String, Object>> list(@PathVariable UUID productId) {
    securityActorResolver.requireAdmin();
    return productImageService.listForProduct(productId).stream()
        .map(ProductImageController::toMap)
        .toList();
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> create(
      @PathVariable UUID productId, @RequestBody Map<String, Object> body) {
    securityActorResolver.requireAdmin();
    String url = body.get("url") == null ? null : String.valueOf(body.get("url"));
    boolean main = Boolean.TRUE.equals(body.get("main"));
    ProductImage image = productImageService.create(productId, url, main);
    return ResponseEntity.status(HttpStatus.CREATED).body(toMap(image));
  }

  @DeleteMapping("/{imageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID productId, @PathVariable UUID imageId) {
    securityActorResolver.requireAdmin();
    productImageService.delete(productId, imageId);
  }

  @PutMapping(value = "/{imageId}/main", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> setMain(@PathVariable UUID productId, @PathVariable UUID imageId) {
    securityActorResolver.requireAdmin();
    return toMap(productImageService.setMain(productId, imageId));
  }

  private static Map<String, Object> toMap(ProductImage image) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", image.getId());
    map.put("product_id", image.getProduct() == null ? null : image.getProduct().getId());
    map.put("url", image.getUrl());
    map.put("main", image.isMain());
    return map;
  }
}
