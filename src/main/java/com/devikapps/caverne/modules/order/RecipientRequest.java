package com.devikapps.caverne.modules.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecipientRequest(
    @NotBlank String location,
    @NotBlank String postal_code,
    @NotBlank String country_code,
    @NotBlank String recipient_name,
    @NotBlank @Email String recipient_email,
    @NotBlank String recipient_phone) {}
