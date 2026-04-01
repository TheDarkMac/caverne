package com.devikapps.caverne.modules.catalog;

import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String listCategories(@RequestParam(defaultValue = "false") boolean flat) {
        List<org.openapitools.client.model.Category> categories = flat
                ? categoryService.getAllCategoriesFlat()
                : categoryService.getCategoriesTree();
        return JSON.getGson().toJson(categories);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String createCategory(@RequestBody String rawBody) {
        return JSON.getGson().toJson(categoryService.createCategory(parseCategoryInput(rawBody)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getCategory(@PathVariable Long id) {
        return JSON.getGson().toJson(categoryService.findById(id));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateCategory(@PathVariable Long id, @RequestBody String rawBody) {
        return JSON.getGson().toJson(categoryService.updateCategory(id, parseCategoryInput(rawBody)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    private org.openapitools.client.model.CategoryInput parseCategoryInput(String rawBody) {
        try {
            return org.openapitools.client.model.CategoryInput.fromJson(rawBody);
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid category payload"
            );
        }
    }
}
