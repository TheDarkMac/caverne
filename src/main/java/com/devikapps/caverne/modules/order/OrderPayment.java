package com.devikapps.caverne.modules.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPayment {

    private Integer paymentId;

    private String methodCode;
    private String currencyCode;
    private BigDecimal amount;
    private LocalDateTime date;
    private String status;

    @Column(name = "internal_reference")
    private String internalReference;

    @Column(columnDefinition = "TEXT")
    private String providerResponse;
}
