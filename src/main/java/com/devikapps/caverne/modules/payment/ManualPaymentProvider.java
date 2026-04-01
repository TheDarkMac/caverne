package com.devikapps.caverne.modules.payment;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ManualPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderCode() {
        return "MANUAL";
    }

    @Override
    public PaymentResponse initiatePayment(BigDecimal amount, String currency, String orderReference) {
        return new PaymentResponse(
                UUID.randomUUID().toString(),
                "PENDING",
                null,
                Map.of("instructions", "Veuillez effectuer le virement sur le compte XXX")
        );
    }
}
