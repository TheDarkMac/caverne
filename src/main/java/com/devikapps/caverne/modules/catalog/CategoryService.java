package com.devikapps.caverne.modules.catalog;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryApiMapper categoryApiMapper;

  public List<org.openapitools.client.model.Category> getCategoriesTree() {
    return categoryRepository.findByParentIsNull().stream()
        .map(categoryApiMapper::toTreeResponse)
        .toList();
  }

  public List<org.openapitools.client.model.Category> getAllCategoriesFlat() {
    return categoryRepository.findAll().stream().map(categoryApiMapper::toFlatResponse).toList();
  }

  public org.openapitools.client.model.Category findById(Long id) {
    return categoryRepository
        .findById(id)
        .map(categoryApiMapper::toTreeResponse)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
  }

  @Transactional
  public org.openapitools.client.model.Category createCategory(
      org.openapitools.client.model.CategoryInput input) {
    Category category = categoryApiMapper.fromInput(input, null);
    return categoryApiMapper.toTreeResponse(categoryRepository.save(category));
  }

  @Transactional
  public org.openapitools.client.model.Category updateCategory(
      Long id, org.openapitools.client.model.CategoryInput input) {
    Category existing =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
    Category category = categoryApiMapper.fromInput(input, existing);
    return categoryApiMapper.toTreeResponse(categoryRepository.save(category));
  }

  @Transactional
  public void delete(Long id) {
    categoryRepository.deleteById(id);
  }
}
