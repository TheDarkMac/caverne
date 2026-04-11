package com.devikapps.caverne.modules.catalog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
  boolean existsByReferenceIgnoreCase(String reference);

  Optional<Product> findByCategoryIdAndLabelIgnoreCase(UUID categoryId, String label);
}
