package com.devikapps.caverne.modules.user;

public record ExternalIdentityProfile(
    AuthProviderCode provider,
    String externalAuthId,
    String email,
    String phone,
    String firstname,
    String lastname) {}
