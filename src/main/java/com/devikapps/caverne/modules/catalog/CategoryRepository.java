package com.devikapps.caverne.modules.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  List<Category> findByParentIsNull();

  Optional<Category> findBySlugIgnoreCase(String slug);
}
