package com.devikapps.caverne.performance;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

public class PublicApiSimulation extends Simulation {

  private static final String RUN_ID = String.valueOf(System.currentTimeMillis());
  private static final String RUN_SUFFIX =
      RUN_ID.substring(Math.max(0, RUN_ID.length() - 6));
  private static final String BASE_URL =
      System.getProperty(
          "gatling.baseUrl",
          System.getenv().getOrDefault("GATLING_BASE_URL", "http://127.0.0.1:8080"));

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL)
          .acceptHeader("application/json")
          .contentTypeHeader("application/json")
          .userAgentHeader("Gatling CI");

  private final ScenarioBuilder publicBrowsingScenario =
      scenario("Public Browsing")
          .exec(
              http("list categories")
                  .get("/categories")
                  .check(status().in(200, 204)))
          .pause(1)
          .exec(
              http("list products")
                  .get("/products")
                  .queryParam("page", "1")
                  .queryParam("per_page", "10")
                  .check(status().is(200)))
          .pause(1)
          .exec(http("list payment methods").get("/payment-methods").check(status().is(200)));

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

  {
    setUp(
            publicBrowsingScenario.injectOpen(rampUsers(5).during(Duration.ofSeconds(10))),
            authFlowScenario.injectOpen(rampUsers(3).during(Duration.ofSeconds(10))))
        .protocols(httpProtocol)
        .assertions(
            global().failedRequests().count().is(0L),
            global().responseTime().max().lt(5000));
  }
}
