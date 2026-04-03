package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAddressService {

  private final UserAddressRepository userAddressRepository;
  private final UserAddressApiMapper userAddressApiMapper;

  @Transactional(readOnly = true)
  public List<org.openapitools.client.model.Address> listForUser(UserAccount user) {
    return userAddressRepository.findAllByUserIdOrderByIdAsc(user.getId()).stream()
        .map(userAddressApiMapper::toResponse)
        .toList();
  }

  public org.openapitools.client.model.Address createForUser(
      UserAccount user, org.openapitools.client.model.AddressInput input) {
    validate(input);
    boolean makeDefault =
        Boolean.TRUE.equals(input.getIsDefault())
            || userAddressRepository.findAllByUserIdOrderByIdAsc(user.getId()).isEmpty();
    if (makeDefault) {
      userAddressRepository.clearDefaultForUser(user.getId());
    }
    UserAddress address =
        UserAddress.builder()
            .user(user)
            .location(input.getLocation().trim())
            .postalCode(input.getPostalCode().trim())
            .countryCode(input.getCountryCode().trim().toUpperCase())
            .isDefault(makeDefault)
            .build();
    return userAddressApiMapper.toResponse(userAddressRepository.save(address));
  }

  public org.openapitools.client.model.Address updateForUser(
      UserAccount user, Long id, org.openapitools.client.model.AddressInput input) {
    validate(input);
    UserAddress address = findOwnedAddress(user, id);
    boolean makeDefault = Boolean.TRUE.equals(input.getIsDefault());
    if (makeDefault) {
      userAddressRepository.clearDefaultForUser(user.getId());
    }
    address.setLocation(input.getLocation().trim());
    address.setPostalCode(input.getPostalCode().trim());
    address.setCountryCode(input.getCountryCode().trim().toUpperCase());
    address.setDefault(makeDefault || address.isDefault());
    return userAddressApiMapper.toResponse(userAddressRepository.save(address));
  }

  public void deleteForUser(UserAccount user, Long id) {
    UserAddress address = findOwnedAddress(user, id);
    boolean wasDefault = address.isDefault();
    userAddressRepository.delete(address);

    if (!wasDefault) {
      return;
    }

    userAddressRepository.findAllByUserIdOrderByIdAsc(user.getId()).stream()
        .findFirst()
        .ifPresent(
            next -> {
              userAddressRepository.clearDefaultForUser(user.getId());
              next.setDefault(true);
              userAddressRepository.save(next);
            });
  }

  public org.openapitools.client.model.Address setDefaultForUser(UserAccount user, Long id) {
    UserAddress address = findOwnedAddress(user, id);
    userAddressRepository.clearDefaultForUser(user.getId());
    address.setDefault(true);
    return userAddressApiMapper.toResponse(userAddressRepository.save(address));
  }

  private UserAddress findOwnedAddress(UserAccount user, Long id) {
    return userAddressRepository
        .findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Address not found"));
  }

  private void validate(org.openapitools.client.model.AddressInput input) {
    if (input == null
        || isBlank(input.getLocation())
        || isBlank(input.getPostalCode())
        || isBlank(input.getCountryCode())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY, "location, postal_code, and country_code are required");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
