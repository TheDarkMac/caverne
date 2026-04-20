package com.devikapps.caverne.modules.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  List<Category> findByParentIsNull();

  Optional<Category> findBySlugIgnoreCase(String slug);

  Optional<Category> findByLabelIgnoreCase(String label);
}
