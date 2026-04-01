package com.devikapps.caverne.modules.catalog;

import java.util.List;

public record CategoryResponse(
    Long id,
    String label,
    String slug,
    String map,
    Long parent_id,
    List<CategoryResponse> children) {}
