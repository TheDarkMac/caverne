package com.devikapps.caverne.modules.catalog;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductImageService {

  private final ProductRepository productRepository;
  private final ProductImageRepository productImageRepository;

  @Transactional(readOnly = true)
  public List<ProductImage> listForProduct(UUID productId) {
    requireProduct(productId);
    return productImageRepository.findAllByProductId(productId);
  }

  @Transactional
  public ProductImage create(UUID productId, String url, boolean main) {
    if (url == null || url.isBlank()) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "url is required");
    }
    Product product = requireProduct(productId);
    ProductImage image =
        ProductImage.builder().product(product).url(url.trim()).isMain(main).build();
    image = productImageRepository.save(image);
    if (main) {
      enforceSingleMain(productId, image.getId());
    }
    return image;
  }

  @Transactional
  public void delete(UUID productId, UUID imageId) {
    ProductImage image = requireImage(productId, imageId);
    productImageRepository.delete(image);
  }

  @Transactional
  public ProductImage setMain(UUID productId, UUID imageId) {
    ProductImage image = requireImage(productId, imageId);
    image.setMain(true);
    productImageRepository.save(image);
    enforceSingleMain(productId, imageId);
    return image;
  }

  private void enforceSingleMain(UUID productId, UUID keepId) {
    List<ProductImage> siblings = productImageRepository.findAllByProductId(productId);
    for (ProductImage sibling : siblings) {
      if (!sibling.getId().equals(keepId) && sibling.isMain()) {
        sibling.setMain(false);
        productImageRepository.save(sibling);
      }
    }
  }

  private Product requireProduct(UUID productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found"));
  }

  private ProductImage requireImage(UUID productId, UUID imageId) {
    ProductImage image =
        productImageRepository
            .findById(imageId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product image not found"));
    if (image.getProduct() == null || !productId.equals(image.getProduct().getId())) {
      throw new ResponseStatusException(NOT_FOUND, "Product image not found");
    }
    return image;
  }
}
