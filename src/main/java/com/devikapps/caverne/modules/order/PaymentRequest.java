package com.devikapps.caverne.modules.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String method_code,
        @NotBlank String currency_code,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
