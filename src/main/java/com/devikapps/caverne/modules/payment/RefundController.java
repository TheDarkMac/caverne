package com.devikapps.caverne.modules.payment;

import com.devikapps.caverne.modules.user.SecurityActorResolver;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class RefundController {

  private final RefundService refundService;
  private final SecurityActorResolver securityActorResolver;

  @PostMapping(value = "/{orderId}/refund", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> refund(
      @PathVariable UUID orderId, @RequestBody(required = false) Map<String, Object> body) {
    securityActorResolver.requireAdmin();
    Number amountValue = body == null ? null : (Number) body.get("amount");
    String reason = body == null || body.get("reason") == null ? null : body.get("reason").toString();
    java.math.BigDecimal amount =
        amountValue == null ? null : new java.math.BigDecimal(amountValue.toString());
    return ResponseEntity.ok(refundService.refundOrder(orderId, amount, reason));
  }
}
