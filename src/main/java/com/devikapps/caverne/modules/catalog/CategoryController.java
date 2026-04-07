package com.devikapps.caverne.modules.catalog;

import com.devikapps.caverne.modules.user.AuthSessionResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;
  private final AuthSessionResolver authSessionResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listCategories(@RequestParam(defaultValue = "false") boolean flat) {
    List<org.openapitools.client.model.Category> categories =
        flat ? categoryService.getAllCategoriesFlat() : categoryService.getCategoriesTree();
    return JSON.getGson().toJson(categories);
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> createCategory(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @RequestBody String rawBody) {
    authSessionResolver.requireAdmin(authorizationHeader);
    CategoryService.UpsertCategoryResult result =
        categoryService.createCategory(parseCategoryInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.category()));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getCategory(@PathVariable Long id) {
    return JSON.getGson().toJson(categoryService.findById(id));
  }

  @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> updateCategory(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @PathVariable Long id,
      @RequestBody String rawBody) {
    authSessionResolver.requireAdmin(authorizationHeader);
    CategoryService.UpsertCategoryResult result =
        categoryService.updateCategory(id, parseCategoryInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.category()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCategory(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @PathVariable Long id) {
    authSessionResolver.requireAdmin(authorizationHeader);
    categoryService.delete(id);
  }

  private org.openapitools.client.model.CategoryInput parseCategoryInput(String rawBody) {
    try {
      return org.openapitools.client.model.CategoryInput.fromJson(rawBody);
    } catch (Exception exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Invalid category payload");
    }
  }
}
