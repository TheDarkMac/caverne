package com.devikapps.caverne.modules.user;

import org.springframework.stereotype.Component;

@Component
public class UserAddressApiMapper {

  public org.openapitools.client.model.Address toResponse(UserAddress address) {
    return new org.openapitools.client.model.Address()
        .id(address.getId().intValue())
        .userId(address.getUser().getId().intValue())
        .location(address.getLocation())
        .postalCode(address.getPostalCode())
        .countryCode(address.getCountryCode())
        .isDefault(address.isDefault());
  }
}
