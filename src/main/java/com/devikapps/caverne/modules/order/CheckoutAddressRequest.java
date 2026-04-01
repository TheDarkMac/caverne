package com.devikapps.caverne.modules.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckoutAddressRequest(
        @NotBlank String location,
        @NotBlank String postal_code,
        @NotBlank String country_code,
        @NotBlank String customer_name,
        @NotBlank @Email String customer_email,
        @NotBlank String customer_phone
) {
}
