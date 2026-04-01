package com.devikapps.caverne.modules.common;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> data,
    PaginatedMeta meta
) {}
