package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.JSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/delivery-costs")
@RequiredArgsConstructor
public class DeliveryCostController {

  private final DeliveryCostService deliveryCostService;
  private final SecurityActorResolver securityActorResolver;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listDeliveryCosts() {
    return JSON.getGson().toJson(deliveryCostService.findAll());
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getDeliveryCost(@PathVariable UUID id) {
    return JSON.getGson().toJson(deliveryCostService.findById(id));
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public String createDeliveryCost(@RequestBody String rawBody) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(deliveryCostService.create(parseInput(rawBody)));
  }

  @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String updateDeliveryCost(@PathVariable UUID id, @RequestBody String rawBody) {
    securityActorResolver.requireAdmin();
    return JSON.getGson().toJson(deliveryCostService.update(id, parseInput(rawBody)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDeliveryCost(@PathVariable UUID id) {
    securityActorResolver.requireAdmin();
    deliveryCostService.delete(id);
  }

  private org.openapitools.client.model.DeliverCostInput parseInput(String rawBody) {
    try {
      return org.openapitools.client.model.DeliverCostInput.fromJson(rawBody);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Invalid delivery cost payload");
    }
  }
}
