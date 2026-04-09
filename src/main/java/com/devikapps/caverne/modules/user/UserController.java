package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserAddressService userAddressService;
  private final SecurityActorResolver securityActorResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listUsers(
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {
    securityActorResolver.requireAdmin();
    var p = userService.findAll(role, PageRequest.of(page - 1, per_page));
    org.openapitools.client.model.UsersGet200Response response =
        new org.openapitools.client.model.UsersGet200Response()
            .data(p.getContent())
            .meta(
                new org.openapitools.client.model.PaginatedMeta()
                    .total(Math.toIntExact(p.getTotalElements()))
                    .page(p.getNumber() + 1)
                    .perPage(p.getSize())
                    .lastPage(p.getTotalPages()));
    return JSON.getGson().toJson(response);
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String createUser(@RequestBody String rawBody) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(userService.createUser(parseUserCreateInput(rawBody)));
  }

  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getMe() {
    return JSON.getGson().toJson(userService.getCurrentUser(securityActorResolver.requireUser()));
  }

  @PutMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateMe(@RequestBody String rawBody) {
    return JSON.getGson()
        .toJson(
            userService.updateCurrentUser(
                securityActorResolver.requireUser(), parseUserUpdate(rawBody)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getUser(@PathVariable Long id) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(userService.getById(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable Long id) {
    securityActorResolver.requireAdmin();
    userService.deleteById(id);
  }

  @GetMapping(value = "/me/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listMyAddresses() {
    return JSON.getGson()
        .toJson(userAddressService.listForUser(securityActorResolver.requireUser()));
  }

  @PostMapping(value = "/me/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> createMyAddress(
      @RequestBody String rawBody) {
    UserAddressService.UpsertAddressResult result =
        userAddressService.createOrUpdateForUser(
            securityActorResolver.requireUser(), parseAddressInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.address()));
  }

  @PutMapping(value = "/me/addresses/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public org.springframework.http.ResponseEntity<String> updateMyAddress(
      @PathVariable Long id, @RequestBody String rawBody) {
    UserAddressService.UpsertAddressResult result =
        userAddressService.updateForUser(
            securityActorResolver.requireUser(), id, parseAddressInput(rawBody));
    return org.springframework.http.ResponseEntity.status(
            result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(JSON.getGson().toJson(result.address()));
  }

  @DeleteMapping("/me/addresses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMyAddress(@PathVariable Long id) {
    userAddressService.deleteForUser(securityActorResolver.requireUser(), id);
  }

  @PutMapping(value = "/me/addresses/{id}/default", produces = MediaType.APPLICATION_JSON_VALUE)
  public String setMyDefaultAddress(@PathVariable Long id) {
    return JSON.getGson()
        .toJson(userAddressService.setDefaultForUser(securityActorResolver.requireUser(), id));
  }

  private org.openapitools.client.model.UserUpdate parseUserUpdate(String rawBody) {
    try {
      return org.openapitools.client.model.UserUpdate.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid user update payload");
    }
  }

  private org.openapitools.client.model.AddressInput parseAddressInput(String rawBody) {
    try {
      return org.openapitools.client.model.AddressInput.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid address payload");
    }
  }

  private org.openapitools.client.model.UserCreateInput parseUserCreateInput(String rawBody) {
    try {
      return org.openapitools.client.model.UserCreateInput.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid user create payload");
    }
  }
}
