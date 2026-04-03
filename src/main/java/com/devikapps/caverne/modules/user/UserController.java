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
  private final AuthSessionResolver authSessionResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listUsers(
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int per_page) {
    authSessionResolver.requireAdmin(authorizationHeader);
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
  public String createUser(
      @RequestHeader("Authorization") String authorizationHeader, @RequestBody String rawBody) {
    authSessionResolver.requireAdmin(authorizationHeader);
    return JSON.getGson().toJson(userService.createUser(parseUserCreateInput(rawBody)));
  }

  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getMe(@RequestHeader("Authorization") String authorizationHeader) {
    return JSON.getGson()
        .toJson(userService.getCurrentUser(authSessionResolver.requireUser(authorizationHeader)));
  }

  @PutMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateMe(
      @RequestHeader("Authorization") String authorizationHeader, @RequestBody String rawBody) {
    return JSON.getGson()
        .toJson(
            userService.updateCurrentUser(
                authSessionResolver.requireUser(authorizationHeader), parseUserUpdate(rawBody)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getUser(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
    authSessionResolver.requireAdmin(authorizationHeader);
    return JSON.getGson().toJson(userService.getById(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
    authSessionResolver.requireAdmin(authorizationHeader);
    userService.deleteById(id);
  }

  @GetMapping(value = "/me/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
  public String listMyAddresses(@RequestHeader("Authorization") String authorizationHeader) {
    return JSON.getGson()
        .toJson(
            userAddressService.listForUser(authSessionResolver.requireUser(authorizationHeader)));
  }

  @PostMapping(value = "/me/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String createMyAddress(
      @RequestHeader("Authorization") String authorizationHeader, @RequestBody String rawBody) {
    return JSON.getGson()
        .toJson(
            userAddressService.createForUser(
                authSessionResolver.requireUser(authorizationHeader), parseAddressInput(rawBody)));
  }

  @PutMapping(value = "/me/addresses/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateMyAddress(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long id,
      @RequestBody String rawBody) {
    return JSON.getGson()
        .toJson(
            userAddressService.updateForUser(
                authSessionResolver.requireUser(authorizationHeader),
                id,
                parseAddressInput(rawBody)));
  }

  @DeleteMapping("/me/addresses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMyAddress(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
    userAddressService.deleteForUser(authSessionResolver.requireUser(authorizationHeader), id);
  }

  @PutMapping(value = "/me/addresses/{id}/default", produces = MediaType.APPLICATION_JSON_VALUE)
  public String setMyDefaultAddress(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
    return JSON.getGson()
        .toJson(
            userAddressService.setDefaultForUser(
                authSessionResolver.requireUser(authorizationHeader), id));
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
