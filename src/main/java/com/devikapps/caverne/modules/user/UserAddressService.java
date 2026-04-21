package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SuppressWarnings("deprecation")
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

  public UpsertAddressResult createOrUpdateForUser(
      UserAccount user, org.openapitools.client.model.AddressInput input) {
    return createOrUpdateForUser(user, input, input.getId());
  }

  public UpsertAddressResult createOrUpdateForUser(
      UserAccount user, org.openapitools.client.model.AddressInput input, UUID requestedId) {
    validate(input);
    UserAddress address =
        requestedId == null
            ? null
            : userAddressRepository.findByIdAndUserId(requestedId, user.getId()).orElse(null);
    boolean created = address == null;
    if (address == null) {
      address = UserAddress.builder().user(user).build();
    }
    boolean makeDefault =
        Boolean.TRUE.equals(input.getIsDefault())
            || (created
                && userAddressRepository.findAllByUserIdOrderByIdAsc(user.getId()).isEmpty());
    if (makeDefault) {
      userAddressRepository.clearDefaultForUser(user.getId());
    }
    address.setLocation(input.getLocation().trim());
    address.setPostalCode(input.getPostalCode().trim());
    address.setCountryCode(input.getCountryCode().trim().toUpperCase());
    address.setDefault(makeDefault || (!created && address.isDefault()));
    return new UpsertAddressResult(
        userAddressApiMapper.toResponse(userAddressRepository.save(address)), created);
  }

  public UpsertAddressResult updateForUser(
      UserAccount user, UUID id, org.openapitools.client.model.AddressInput input) {
    if (input.getId() != null && !id.equals(input.getId())) {
      throw new ResponseStatusException(
          UNPROCESSABLE_CONTENT, "Address payload id does not match path id");
    }
    return createOrUpdateForUser(user, input, id);
  }

  public void deleteForUser(UserAccount user, UUID id) {
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

  public org.openapitools.client.model.Address setDefaultForUser(UserAccount user, UUID id) {
    UserAddress address = findOwnedAddress(user, id);
    userAddressRepository.clearDefaultForUser(user.getId());
    address.setDefault(true);
    return userAddressApiMapper.toResponse(userAddressRepository.save(address));
  }

  private UserAddress findOwnedAddress(UserAccount user, UUID id) {
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
          UNPROCESSABLE_CONTENT, "location, postal_code, and country_code are required");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record UpsertAddressResult(
      org.openapitools.client.model.Address address, boolean created) {}
}
