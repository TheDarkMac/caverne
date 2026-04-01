package com.devikapps.caverne.modules.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull Long product_id,
        @NotNull @DecimalMin("0.0001") BigDecimal quantity
) {
}
