package com.devikapps.caverne.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PublicApiSimulation extends Simulation {

  private static final String RUN_ID = String.valueOf(System.currentTimeMillis());
  private static final String RUN_SUFFIX = RUN_ID.substring(Math.max(0, RUN_ID.length() - 6));
  private static final String BASE_URL =
      System.getProperty(
          "gatling.baseUrl",
          System.getenv().getOrDefault("GATLING_BASE_URL", "http://127.0.0.1:8080/api/v1"));

  // Tunable load via -D flags or env vars so local runs can go bigger.
  private static final int BROWSE_USERS = intProp("gatling.users.browse", 50);
  private static final int BROWSE_RATE = intProp("gatling.rate.browse", 10);
  private static final int BROWSE_RATE_DURATION = intProp("gatling.duration.browse", 60);
  private static final int AUTH_USERS = intProp("gatling.users.auth", 20);
  private static final int CHECKOUT_USERS = intProp("gatling.users.checkout", 10);
  private static final int CHECKOUT_RATE = intProp("gatling.rate.checkout", 2);
  private static final int CHECKOUT_RATE_DURATION = intProp("gatling.duration.checkout", 60);

  private static final List<String> SEARCH_TERMS =
      List.of("coffee", "tea", "bean", "chocolate", "spice");

  private static int intProp(String key, int fallback) {
    String value = System.getProperty(key, System.getenv(key.toUpperCase().replace('.', '_')));
    if (value == null || value.isBlank()) return fallback;
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException nfe) {
      return fallback;
    }
  }

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL)
          .acceptHeader("application/json")
          .contentTypeHeader("application/json")
          .userAgentHeader("Gatling CI");

  private static final Duration THINK_MIN = Duration.ofMillis(500);
  private static final Duration THINK_MAX = Duration.ofSeconds(2);

  // ---------- Existing simple public browsing (kept, lightly enhanced) ----------
  private final ScenarioBuilder publicBrowsingScenario =
      scenario("Public Browsing")
          .exec(http("list categories").get("/categories").check(status().in(200, 204)))
          .pause(THINK_MIN, THINK_MAX)
          .exec(
              http("list products")
                  .get("/products")
                  .queryParam("page", "1")
                  .queryParam("per_page", "20")
                  .check(status().is(200))
                  .check(
                      jsonPath("$.data[*].id").findRandom().optional().saveAs("randomProductId")))
          .pause(THINK_MIN, THINK_MAX)
          .exec(
              http("product detail")
                  .get(
                      session ->
                          "/products/"
                              + (session.contains("randomProductId")
                                  ? session.getString("randomProductId")
                                  : "00000000-0000-0000-0000-000000000000"))
                  .check(status().in(200, 404)))
          .pause(THINK_MIN, THINK_MAX)
          .exec(
              http("list products page 2")
                  .get("/products")
                  .queryParam("page", "2")
                  .queryParam("per_page", "20")
                  .check(status().is(200)))
          .pause(THINK_MIN, THINK_MAX)
          .exec(
              session -> {
                String term =
                    SEARCH_TERMS.get(ThreadLocalRandom.current().nextInt(SEARCH_TERMS.size()));
                return session.set("searchTerm", term);
              })
          .exec(
              http("search products")
                  .get("/products")
                  .queryParam("search", "#{searchTerm}")
                  .queryParam("page", "1")
                  .queryParam("per_page", "20")
                  .check(status().is(200)))
          .pause(THINK_MIN, THINK_MAX)
          .exec(http("list delivery costs").get("/delivery-costs").check(status().is(200)))
          .pause(THINK_MIN, THINK_MAX)
          .exec(http("list payment methods").get("/payment-methods").check(status().is(200)));

  // ---------- Auth flow (kept from previous version) ----------
  private final ScenarioBuilder authFlowScenario =
      scenario("Auth Flow")
          .exec(
              session -> {
                long userId = session.userId();
                return session
                    .set("email", "gatling+" + RUN_ID + "-" + userId + "@example.com")
                    .set("phone", "+26134" + RUN_SUFFIX + String.format("%02d", userId % 100));
              })
          .exec(
              http("register")
                  .post("/auth/register")
                  .body(
                      StringBody(
                          session ->
                              """
                              {
                                "firstname":"Gatling",
                                "lastname":"User",
                                "email":"%s",
                                "phone":"%s",
                                "password":"secret123"
                              }
                              """
                                  .formatted(
                                      session.getString("email"), session.getString("phone"))))
                  .check(status().is(201)))
          .pause(1)
          .exec(
              http("login")
                  .post("/auth/login")
                  .body(
                      StringBody(
                          session ->
                              """
                              {
                                "email":"%s",
                                "password":"secret123"
                              }
                              """
                                  .formatted(session.getString("email"))))
                  .check(status().is(200))
                  .check(jsonPath("$.access_token").saveAs("accessToken")))
          .pause(1)
          .exec(
              http("get current user")
                  .get("/users/me")
                  .header("Authorization", "Bearer #{accessToken}")
                  .check(status().is(200)))
          .pause(1)
          .exec(
              http("list own orders")
                  .get("/orders")
                  .header("Authorization", "Bearer #{accessToken}")
                  .queryParam("page", "1")
                  .queryParam("per_page", "10")
                  .check(status().is(200)));

  // ---------- Checkout (guest, end-to-end write-heavy) ----------
  // Guest orders are permitted (see SecurityConfiguration: POST /orders is permitAll).
  // Note: payment_method selection happens via POST /orders/{id}/payments, NOT OrderInput itself
  // (OrderInput has no payment_method_id field — verified against clients/api/openapi.yaml).
  // Note: admin-only endpoints (PUT /orders/*/status, GET /orders/all, GET /products/*/stock,
  // POST /categories, POST /users, etc.) are intentionally skipped — not load-testable as guest.
  private final ChainBuilder pickProduct =
      exec(
          http("list products (checkout)")
              .get("/products")
              .queryParam("page", "1")
              .queryParam("per_page", "20")
              .check(status().is(200))
              .check(jsonPath("$.data[*].id").findRandom().saveAs("productId")));

  private final ChainBuilder pickDelivery =
      exec(
          http("list delivery costs (checkout)")
              .get("/delivery-costs")
              .check(status().is(200))
              .check(jsonPath("$[*].id").findRandom().optional().saveAs("deliveryCostId")));

  private final ChainBuilder pickPaymentMethod =
      exec(
          http("list payment methods (checkout)")
              .get("/payment-methods")
              .check(status().is(200))
              .check(
                  jsonPath("$[*].provider_code")
                      .findRandom()
                      .optional()
                      .saveAs("paymentProvider")));

  private final ScenarioBuilder checkoutScenario =
      scenario("Checkout")
          .exec(pickProduct)
          .pause(THINK_MIN, THINK_MAX)
          .exec(pickDelivery)
          .pause(Duration.ofMillis(200), Duration.ofMillis(800))
          .exec(pickPaymentMethod)
          .pause(THINK_MIN, THINK_MAX)
          .exec(
              session -> {
                long uid = session.userId();
                String email = "guest+" + RUN_ID + "-" + uid + "@example.com";
                String phone = "+26135" + RUN_SUFFIX + String.format("%02d", uid % 100);
                return session.set("guestEmail", email).set("guestPhone", phone);
              })
          .exec(
              http("create order")
                  .post("/orders")
                  .body(
                      StringBody(
                          session -> {
                            String productId = session.getString("productId");
                            String deliveryFragment =
                                session.contains("deliveryCostId")
                                        && session.getString("deliveryCostId") != null
                                    ? "\"delivery_cost_id\":\""
                                        + session.getString("deliveryCostId")
                                        + "\","
                                    : "";
                            return """
                                   {
                                     %s
                                     "currency_code":"XAF",
                                     "items":[
                                       {"product_id":"%s","quantity":1}
                                     ],
                                     "recipient":{
                                       "country_code":"CMR",
                                       "location":"Douala",
                                       "postal_code":"00237",
                                       "recipient_name":"Gatling Guest",
                                       "recipient_email":"%s",
                                       "recipient_phone":"%s"
                                     }
                                   }
                                   """
                                .formatted(
                                    deliveryFragment,
                                    productId,
                                    session.getString("guestEmail"),
                                    session.getString("guestPhone"));
                          }))
                  .check(status().in(201, 422))
                  .check(jsonPath("$.id").optional().saveAs("orderId")))
          .pause(Duration.ofMillis(300), Duration.ofSeconds(1))
          .doIf(session -> session.contains("orderId"))
          .then(exec(http("get order").get("/orders/#{orderId}").check(status().in(200, 404))));

  {
    setUp(
            publicBrowsingScenario.injectOpen(
                rampUsers(BROWSE_USERS).during(Duration.ofSeconds(30)),
                constantUsersPerSec(BROWSE_RATE).during(Duration.ofSeconds(BROWSE_RATE_DURATION))),
            authFlowScenario.injectOpen(rampUsers(AUTH_USERS).during(Duration.ofSeconds(30))),
            checkoutScenario.injectOpen(
                rampUsers(CHECKOUT_USERS).during(Duration.ofSeconds(30)),
                constantUsersPerSec(CHECKOUT_RATE)
                    .during(Duration.ofSeconds(CHECKOUT_RATE_DURATION))))
        .protocols(httpProtocol)
        .assertions(
            // Global sanity caps
            global().failedRequests().percent().lt(1.0),
            global().responseTime().max().lt(5000),

            // Read-path SLOs
            details("list products").responseTime().percentile(95.0).lt(500),
            details("list products").responseTime().percentile(99.0).lt(1000),
            details("product detail").responseTime().percentile(95.0).lt(500),
            details("product detail").responseTime().percentile(99.0).lt(1000),
            details("list categories").responseTime().percentile(95.0).lt(500),
            details("list categories").responseTime().percentile(99.0).lt(1000),

            // Checkout SLOs (heaviest transactional path)
            details("create order").responseTime().percentile(95.0).lt(2000),
            details("create order").successfulRequests().percent().gt(98.0),

            // Auth SLOs
            details("register").responseTime().percentile(95.0).lt(1500),
            details("login").responseTime().percentile(95.0).lt(1500));
  }
}
