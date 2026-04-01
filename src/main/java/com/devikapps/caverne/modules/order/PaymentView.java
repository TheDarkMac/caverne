package com.devikapps.caverne.modules.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record PaymentView(
        String method_code,
        String currency_code,
        BigDecimal amount,
        LocalDateTime date,
        String status,
        String internal_reference,
        Map<String, Object> provider_response
) {
}
