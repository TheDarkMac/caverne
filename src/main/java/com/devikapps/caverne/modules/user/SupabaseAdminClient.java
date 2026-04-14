package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "auth.providers.supabase.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SupabaseAdminClient {

  private static final MediaType JSON = MediaType.get("application/json");

  private final SupabaseAuthProperties properties;
  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient = new OkHttpClient();

  public String createUser(
      String email, String phone, String password, String firstName, String lastName) {
    ObjectNode body = objectMapper.createObjectNode();
    if (email != null) body.put("email", email);
    if (phone != null) body.put("phone", phone);
    body.put("password", password);
    body.put("email_confirm", true);

    ObjectNode metadata = objectMapper.createObjectNode();
    if (firstName != null) metadata.put("first_name", firstName);
    if (lastName != null) metadata.put("last_name", lastName);
    body.set("user_metadata", metadata);

    String url = properties.getUrl().stripTrailing() + "/auth/v1/admin/users";
    String key = properties.getServiceRoleKey();

    Request request;
    try {
      request =
          new Request.Builder()
              .url(url)
              .addHeader("apikey", key)
              .addHeader("Authorization", "Bearer " + key)
              .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
              .build();
    } catch (IOException e) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Failed to build Supabase request");
    }

    try (Response response = httpClient.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      if (!response.isSuccessful()) {
        String message = extractMessage(responseBody);
        throw new ResponseStatusException(
            UNPROCESSABLE_ENTITY, "Supabase registration failed: " + message);
      }
      JsonNode json = objectMapper.readTree(responseBody);
      String id = json.path("id").asText(null);
      if (id == null || id.isBlank()) {
        throw new ResponseStatusException(
            UNPROCESSABLE_ENTITY, "Supabase returned no user id");
      }
      return id;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Supabase is unreachable");
    }
  }

  private String extractMessage(String body) {
    try {
      JsonNode json = objectMapper.readTree(body);
      String msg = json.path("msg").asText(null);
      if (msg == null) msg = json.path("message").asText(null);
      return msg != null ? msg : body;
    } catch (IOException e) {
      return body;
    }
  }
}
