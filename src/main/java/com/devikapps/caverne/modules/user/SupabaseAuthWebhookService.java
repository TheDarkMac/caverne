package com.devikapps.caverne.modules.user;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "auth.providers.supabase.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SupabaseAuthWebhookService {

  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;

  @Transactional
  public void handle(JsonNode payload) {
    if (payload == null) {
      return;
    }
    String type = textOrNull(payload.get("type"));
    String table = textOrNull(payload.get("table"));
    String schema = textOrNull(payload.get("schema"));

    if (table != null && !"users".equalsIgnoreCase(table)) {
      return;
    }
    if (schema != null && !"auth".equalsIgnoreCase(schema)) {
      return;
    }
    if (type == null) {
      return;
    }

    JsonNode record = payload.get("record");
    JsonNode oldRecord = payload.get("old_record");

    switch (type.toUpperCase()) {
      case "INSERT" -> upsertFromRecord(record);
      case "UPDATE" -> updateFromRecord(record);
      case "DELETE" -> deleteFromRecord(oldRecord != null ? oldRecord : record);
      default -> {
      }
    }
  }

  private void upsertFromRecord(JsonNode record) {
    if (record == null) {
      return;
    }
    String externalId = textOrNull(record.get("id"));
    if (externalId == null) {
      return;
    }
    String email = normalize(textOrNull(record.get("email")));
    String phone = normalize(textOrNull(record.get("phone")));

    UserAccount user =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, externalId)
            .orElse(null);

    if (user == null && email != null) {
      user = userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    if (user == null) {
      user =
          UserAccount.builder()
              .firstname(firstName(record))
              .lastname(lastName(record))
              .email(email)
              .phone(phone)
              .passwordHash("")
              .authProvider(AuthProviderCode.SUPABASE)
              .externalAuthId(externalId)
              .role(UserRole.SIMPLE_USER)
              .status("active")
              .build();
    } else {
      user.setAuthProvider(AuthProviderCode.SUPABASE);
      user.setExternalAuthId(externalId);
      if (email != null) {
        user.setEmail(email);
      }
      if (phone != null) {
        user.setPhone(phone);
      }
      if (user.getStatus() == null || user.getStatus().isBlank()) {
        user.setStatus("active");
      }
    }
    userRepository.save(user);
  }

  private void updateFromRecord(JsonNode record) {
    if (record == null) {
      return;
    }
    String externalId = textOrNull(record.get("id"));
    if (externalId == null) {
      return;
    }
    UserAccount user =
        userRepository
            .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, externalId)
            .orElse(null);
    if (user == null) {
      // upsert if missing
      upsertFromRecord(record);
      return;
    }
    String email = normalize(textOrNull(record.get("email")));
    String phone = normalize(textOrNull(record.get("phone")));
    if (email != null) {
      user.setEmail(email);
    }
    if (phone != null) {
      user.setPhone(phone);
    }
    JsonNode banned = record.get("banned_until");
    if (banned != null && !banned.isNull() && !banned.asText().isBlank()) {
      user.setStatus("banned");
    }
    userRepository.save(user);
  }

  private void deleteFromRecord(JsonNode record) {
    if (record == null) {
      return;
    }
    String externalId = textOrNull(record.get("id"));
    if (externalId == null) {
      return;
    }
    userRepository
        .findByAuthProviderAndExternalAuthId(AuthProviderCode.SUPABASE, externalId)
        .ifPresent(
            user -> {
              authSessionRepository.deleteByUser(user);
              userRepository.delete(user);
            });
  }

  private String firstName(JsonNode record) {
    JsonNode meta = record.get("raw_user_meta_data");
    if (meta != null) {
      String f = textOrNull(meta.get("first_name"));
      if (f != null) return f;
      String g = textOrNull(meta.get("given_name"));
      if (g != null) return g;
    }
    return "Supabase";
  }

  private String lastName(JsonNode record) {
    JsonNode meta = record.get("raw_user_meta_data");
    if (meta != null) {
      String l = textOrNull(meta.get("last_name"));
      if (l != null) return l;
      String f = textOrNull(meta.get("family_name"));
      if (f != null) return f;
    }
    return "User";
  }

  private String textOrNull(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    String text = node.asText();
    return text == null || text.isBlank() ? null : text;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
