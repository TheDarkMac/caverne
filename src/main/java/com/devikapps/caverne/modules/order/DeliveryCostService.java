package com.devikapps.caverne.modules.order;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryCostService {

  private final DeliveryCostRepository deliveryCostRepository;
  private final ObjectMapper objectMapper;

  public List<org.openapitools.client.model.DeliverCost> findAll() {
    return deliveryCostRepository.findAll().stream().map(this::toModel).toList();
  }

  public org.openapitools.client.model.DeliverCost findById(Long id) {
    return toModel(findEntityById(id));
  }

  @Transactional
  public org.openapitools.client.model.DeliverCost create(
      org.openapitools.client.model.DeliverCostInput input) {
    return toModel(deliveryCostRepository.save(fromInput(input, null)));
  }

  @Transactional
  public org.openapitools.client.model.DeliverCost update(
      Long id, org.openapitools.client.model.DeliverCostInput input) {
    DeliveryCost existing = findEntityById(id);
    return toModel(deliveryCostRepository.save(fromInput(input, existing)));
  }

  @Transactional
  public void delete(Long id) {
    deliveryCostRepository.delete(findEntityById(id));
  }

  public DeliveryCost requireEntityById(Long id) {
    return findEntityById(id);
  }

  private DeliveryCost findEntityById(Long id) {
    return deliveryCostRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Delivery cost not found"));
  }

  private DeliveryCost fromInput(
      org.openapitools.client.model.DeliverCostInput input, DeliveryCost existing) {
    if (input == null
        || input.getAmount() == null
        || input.getAmount() <= 0
        || isBlank(input.getProvider())) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "delivery cost input is invalid");
    }

    DeliveryCost deliveryCost = existing == null ? new DeliveryCost() : existing;
    deliveryCost.setAmount(java.math.BigDecimal.valueOf(input.getAmount()));
    deliveryCost.setProvider(input.getProvider().trim());
    return deliveryCost;
  }

  private org.openapitools.client.model.DeliverCost toModel(DeliveryCost deliveryCost) {
    return new org.openapitools.client.model.DeliverCost()
        .id(deliveryCost.getId().intValue())
        .amount(deliveryCost.getAmount().doubleValue())
        .provider(deliveryCost.getProvider())
        .responseProvider(readResponseProvider(deliveryCost.getResponseProvider()));
  }

  private Object readResponseProvider(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(raw, Object.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to read delivery cost response provider");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
