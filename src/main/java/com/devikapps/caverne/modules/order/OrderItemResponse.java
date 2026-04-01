package com.devikapps.caverne.modules.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long product_id, BigDecimal quantity, BigDecimal unit_price) {}
