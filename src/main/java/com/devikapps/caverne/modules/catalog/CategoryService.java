package com.devikapps.caverne.modules.catalog;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.List;
import java.util.UUID;
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

  public org.openapitools.client.model.Category findById(UUID id) {
    return categoryRepository
        .findById(id)
        .map(categoryApiMapper::toTreeResponse)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
  }

  @Transactional
  public UpsertCategoryResult createCategory(org.openapitools.client.model.CategoryInput input) {
    if (input.getId() != null) {
      Category existing =
          categoryRepository
              .findById(input.getId())
              .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
      Category category = categoryApiMapper.fromInput(input, existing);
      return new UpsertCategoryResult(
          categoryApiMapper.toTreeResponse(categoryRepository.save(category)), false);
    }

    Category category = categoryApiMapper.fromInput(input, null);
    return new UpsertCategoryResult(
        categoryApiMapper.toTreeResponse(categoryRepository.save(category)), true);
  }

  @Transactional
  public UpsertCategoryResult updateCategory(
      UUID id, org.openapitools.client.model.CategoryInput input) {
    if (input.getId() != null && !id.equals(input.getId())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "Category payload id does not match path id");
    }

    Category existing = categoryRepository.findById(id).orElse(null);
    Category category = categoryApiMapper.fromInput(input, existing);

    return new UpsertCategoryResult(
        categoryApiMapper.toTreeResponse(categoryRepository.save(category)), existing == null);
  }

  @Transactional
  public void delete(UUID id) {
    categoryRepository.deleteById(id);
  }

  public record UpsertCategoryResult(
      org.openapitools.client.model.Category category, boolean created) {}
}
