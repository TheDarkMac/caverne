package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final SecurityActorResolver securityActorResolver;

  @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String register(@RequestBody String rawBody) {
    return JSON.getGson().toJson(authService.register(parseRegisterRequest(rawBody)));
  }

  @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
  public String login(@RequestBody String rawBody) {
    return JSON.getGson().toJson(authService.login(parseLoginRequest(rawBody)));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    authService.logout(securityActorResolver.requireBearerToken());
  }

  private org.openapitools.client.model.RegisterRequest parseRegisterRequest(String rawBody) {
    try {
      return org.openapitools.client.model.RegisterRequest.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid registration payload");
    }
  }

  private org.openapitools.client.model.LoginRequest parseLoginRequest(String rawBody) {
    try {
      return org.openapitools.client.model.LoginRequest.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid login payload");
    }
  }
}
