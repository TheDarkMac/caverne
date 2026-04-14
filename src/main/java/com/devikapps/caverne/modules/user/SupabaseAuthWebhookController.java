package com.devikapps.caverne.modules.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/webhooks")
@ConditionalOnProperty(name = "auth.providers.supabase.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SupabaseAuthWebhookController {

  private final SupabaseAuthProperties supabaseAuthProperties;
  private final SupabaseAuthWebhookService supabaseAuthWebhookService;
  private final ObjectMapper objectMapper;

  @PostMapping(value = "/supabase", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> handle(
      @RequestHeader(value = "X-Webhook-Secret", required = false) String providedSecret,
      @RequestBody String body)
      throws Exception {
    String configuredSecret = supabaseAuthProperties.getWebhookSecret();
    if (configuredSecret == null || configuredSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Supabase webhook secret is not configured");
    }
    if (providedSecret == null || !constantTimeEquals(configuredSecret, providedSecret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
    }
    JsonNode payload = objectMapper.readTree(body);
    supabaseAuthWebhookService.handle(payload);
    return ResponseEntity.noContent().build();
  }

  private static boolean constantTimeEquals(String a, String b) {
    byte[] ab = a.getBytes(StandardCharsets.UTF_8);
    byte[] bb = b.getBytes(StandardCharsets.UTF_8);
    if (ab.length != bb.length) {
      // still call MessageDigest.isEqual on padded buffer to keep timing similar
      byte[] pad = new byte[Math.max(ab.length, bb.length)];
      MessageDigest.isEqual(pad, pad);
      return false;
    }
    return MessageDigest.isEqual(ab, bb);
  }
}
