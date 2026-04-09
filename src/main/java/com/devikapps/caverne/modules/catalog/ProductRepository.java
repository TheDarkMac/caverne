package com.devikapps.caverne.modules.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  boolean existsByReferenceIgnoreCase(String reference);

  Optional<Product> findByCategoryIdAndLabelIgnoreCase(Long categoryId, String label);
}
