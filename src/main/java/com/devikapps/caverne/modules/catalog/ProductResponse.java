package com.devikapps.caverne.modules.catalog;

import java.time.LocalDate;
import java.util.List;

public record ProductResponse(
        Long id,
        Long category_id,
        String label,
        String reference,
        LocalDate limit_date,
        String description,
        String size,
        boolean is_active,
        List<PriceResponse> prices
) {
}
