package com.devikapps.caverne.modules.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderView(
    Long id,
    String currency_code,
    String reference,
    LocalDateTime date,
    String status,
    List<OrderItemResponse> items,
    RecipientRequest recipient,
    List<PaymentView> payments) {}
