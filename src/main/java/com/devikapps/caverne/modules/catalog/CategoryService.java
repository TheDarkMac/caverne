package com.devikapps.caverne.modules.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<org.openapitools.client.model.Category> getCategoriesTree() {
        return categoryRepository.findByParentIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<org.openapitools.client.model.Category> getAllCategoriesFlat() {
        return categoryRepository.findAll().stream()
                .map(this::toFlatResponse)
                .toList();
    }

    public org.openapitools.client.model.Category findById(Long id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
    }

    @Transactional
    public org.openapitools.client.model.Category createCategory(org.openapitools.client.model.CategoryInput input) {
        Category category = fromInput(input, null);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public org.openapitools.client.model.Category updateCategory(Long id, org.openapitools.client.model.CategoryInput input) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
        Category category = fromInput(input, existing);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    private org.openapitools.client.model.Category toResponse(Category category) {
        return new org.openapitools.client.model.Category()
                .id(category.getId().intValue())
                .label(category.getLabel())
                .slug(category.getSlug())
                .map(category.getMap())
                .parentId(category.getParent() == null ? null : category.getParent().getId().intValue())
                .children(category.getChildren() == null ? List.of() : category.getChildren().stream()
                        .map(this::toResponse)
                        .toList());
    }

    private org.openapitools.client.model.Category toFlatResponse(Category category) {
        return new org.openapitools.client.model.Category()
                .id(category.getId().intValue())
                .label(category.getLabel())
                .slug(category.getSlug())
                .map(category.getMap())
                .parentId(category.getParent() == null ? null : category.getParent().getId().intValue())
                .children(List.of());
    }

    private Category fromInput(org.openapitools.client.model.CategoryInput input, Category existing) {
        Category category = existing == null ? new Category() : existing;
        category.setLabel(input.getLabel());
        category.setSlug(input.getSlug());
        category.setMap(input.getMap());

        if (input.getParentId() != null) {
            Category parent = new Category();
            parent.setId(input.getParentId().longValue());
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return category;
    }
}
