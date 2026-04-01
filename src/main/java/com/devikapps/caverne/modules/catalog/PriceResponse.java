package com.devikapps.caverne.modules.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
        Long id,
        Long product_id,
        String currency_code,
        BigDecimal value,
        LocalDate valid_from,
        String unit
) {
}
