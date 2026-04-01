package com.devikapps.caverne.modules.payment;

import java.util.Map;

public record PaymentResponse(
    String transactionId, String status, String paymentUrl, Map<String, Object> providerData) {}
