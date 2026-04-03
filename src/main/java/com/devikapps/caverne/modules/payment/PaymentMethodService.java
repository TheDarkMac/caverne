package com.devikapps.caverne.modules.payment;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.openapitools.client.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMethodService {

  private final List<PaymentProvider> paymentProviders;

  private static final Map<String, PaymentMethodDescriptor> DESCRIPTORS =
      Map.of(
          "MANUAL",
          new PaymentMethodDescriptor(
              "MANUAL",
              "Paiement manuel (virement bancaire)",
              true,
              PaymentMethod.IntegrationTypeEnum.MANUAL),
          "STRIPE",
          new PaymentMethodDescriptor(
              "STRIPE", "Carte bancaire (Stripe)", false, PaymentMethod.IntegrationTypeEnum.API));

  public List<PaymentMethod> listAll() {
    return paymentProviders.stream()
        .map(PaymentProvider::getProviderCode)
        .filter(Objects::nonNull)
        .map(String::toUpperCase)
        .distinct()
        .sorted()
        .map(this::toPaymentMethod)
        .collect(toList());
  }

  private PaymentMethod toPaymentMethod(String providerCode) {
    PaymentMethodDescriptor descriptor = DESCRIPTORS.get(providerCode);
    if (descriptor == null) {
      return new PaymentMethod().providerCode(providerCode).isActive(true);
    }
    return new PaymentMethod()
        .providerCode(descriptor.providerCode())
        .label(descriptor.label())
        .isActive(true)
        .requiresManualCheck(descriptor.requiresManualCheck())
        .integrationType(descriptor.integrationType());
  }

  private record PaymentMethodDescriptor(
      String providerCode,
      String label,
      boolean requiresManualCheck,
      PaymentMethod.IntegrationTypeEnum integrationType) {}
}
