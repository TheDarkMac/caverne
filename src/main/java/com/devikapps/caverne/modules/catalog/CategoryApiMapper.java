package com.devikapps.caverne.modules.catalog;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryApiMapper {

  public org.openapitools.client.model.Category toTreeResponse(Category category) {
    return new org.openapitools.client.model.Category()
        .id(category.getId().intValue())
        .label(category.getLabel())
        .slug(category.getSlug())
        .map(category.getMap())
        .parentId(category.getParent() == null ? null : category.getParent().getId().intValue())
        .children(
            category.getChildren() == null
                ? List.of()
                : category.getChildren().stream().map(this::toTreeResponse).toList());
  }

  public org.openapitools.client.model.Category toFlatResponse(Category category) {
    return new org.openapitools.client.model.Category()
        .id(category.getId().intValue())
        .label(category.getLabel())
        .slug(category.getSlug())
        .map(category.getMap())
        .parentId(category.getParent() == null ? null : category.getParent().getId().intValue())
        .children(List.of());
  }

  public Category fromInput(org.openapitools.client.model.CategoryInput input, Category existing) {
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
