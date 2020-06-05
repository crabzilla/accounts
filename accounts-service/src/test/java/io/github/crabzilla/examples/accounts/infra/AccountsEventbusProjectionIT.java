package io.github.crabzilla.examples.accounts.infra;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.*;
import static io.github.crabzilla.examples.accounts.infra.Db_boilerplateKt.cleanDatabase;
import static io.github.crabzilla.examples.accounts.infra.boilerplate.ConfigSupport.getConfig;
import static io.github.crabzilla.examples.accounts.infra.boilerplate.DeploySupport.deploy;
import static io.github.crabzilla.examples.accounts.infra.boilerplate.HttpSupport.findFreeHttpPort;
import static io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.deploySingleton;
import static io.vertx.junit5.web.TestRequest.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing success scenarios
 **/
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsEventbusProjectionIT {

  private static final Logger log = LoggerFactory.getLogger(AccountsEventbusProjectionIT.class);
  private static WebClient readWebClient;
  private static WebClient writeWebClient;
  private static final Random random = new Random();
  private static int randomAcctId = random.nextInt();

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    getConfig(vertx, "./../accounts.env")
      .onFailure(tc::failNow)
      .onSuccess(config -> {
          config.put("WRITE_HTTP_PORT", findFreeHttpPort());
          config.put("READ_HTTP_PORT", findFreeHttpPort() + 1);
          writeWebClient = create(vertx, config.getInteger("WRITE_HTTP_PORT"));
          readWebClient = create(vertx, config.getInteger("READ_HTTP_PORT"));
          DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config).setInstances(1);
          CompositeFuture.all(
                  deploy(vertx, WebCommandVerticle.class.getName(), deploymentOptions),
                  deploy(vertx, WebQueryVerticle.class.getName(), deploymentOptions),
                  deploySingleton(vertx, new DbProjectionsVerticle(), deploymentOptions, "test")
          ).onSuccess(ok -> cleanDatabase(vertx, config)
                  .onSuccess(ok2 -> tc.completeNow())
                  .onFailure(tc::failNow)
          ).onFailure(tc::failNow);
        }
      );
  }

  static WebClient create(Vertx vertx, int httpPort) {
    WebClientOptions wco = new WebClientOptions();
    wco.setDefaultPort(httpPort);
    wco.setDefaultHost("0.0.0.0");
    return WebClient.create(vertx, wco);
  }

  @Nested
  @DisplayName("When making a $10 withdraw on a missing account")
  class When5 {
    @Test
    @DisplayName("You get a bad request")
    void a7(VertxTestContext tc) {
      JsonObject cmdAsJson = new JsonObject("{\"amount\" : 10.00}");
      testRequest(writeWebClient, HttpMethod.POST, "/commands/account/" + random.nextInt() + "/make-withdraw")
              .expect(statusCode(400))
              .expect(statusMessage("This account must exists"))
              .sendJson(cmdAsJson, tc);
    }

    @Nested
    @DisplayName("When making a $10 deposit on a new account")
    class When1 {

      @Test
      @DisplayName("You get version 1 and events = AccountCreated and $10 AmountDeposit")
      void a1(VertxTestContext tc) {
        JsonObject cmdAsJson = new JsonObject("{\"amount\" : 10.00}");
        testRequest(writeWebClient, HttpMethod.POST, "/commands/account/" + randomAcctId + "/make-deposit")
                .expect(statusCode(200))
                .expect(statusMessage("OK"))
                .expect(response -> {
                  JsonObject result = response.bodyAsJsonObject();
                  Long uowId = new Long(response.getHeader("uowId"));
                  assertThat(uowId).isPositive();
                  assertThat(result.getString(ENTITY_NAME)).isEqualTo("account");
                  assertThat(result.getInteger(ENTITY_ID)).isEqualTo(randomAcctId);
                  assertThat(result.getString(COMMAND_ID)).isNotNull();
                  assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
                  assertThat(result.getInteger(VERSION)).isEqualTo(1);
                  assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(2);
                  assertThat(result.getJsonArray(EVENTS).getJsonObject(0)
                          .getJsonObject("accountId").getDouble("value")).isEqualTo(randomAcctId);
                  assertThat(result.getJsonArray(EVENTS).getJsonObject(1)
                          .getDouble("amount")).isEqualTo(10.00);
                })
                .sendJson(cmdAsJson, tc);
      }

      @Test
      @DisplayName("You get account summary with balance = 10.00")
      void a2(VertxTestContext tc) {
        readWebClient.get("/accounts/" + randomAcctId)
            .as(BodyCodec.jsonObject())
            .expect(ResponsePredicate.SC_SUCCESS)
//            .expect(ResponsePredicate.JSON)
            .send(tc.succeeding(response -> tc.verify(() -> {
                      JsonObject result = response.body();
                      assertThat(result.getInteger("id")).isEqualTo(randomAcctId);
                      assertThat(result.getDouble("balance")).isEqualTo(10.00);
                      tc.completeNow();
                    }))
            );
      }

      @Test
      @DisplayName("You get accounts with just this account")
      void a22(VertxTestContext tc) {
        readWebClient.get("/accounts")
          .as(BodyCodec.jsonArray())
          .expect(ResponsePredicate.SC_SUCCESS)
          .send(tc.succeeding(response -> tc.verify(() -> {
              JsonArray result = response.body();
              assertThat(result.size()).isEqualTo(1);
              JsonObject account = result.getJsonObject(0);
              assertThat(account.getInteger("id")).isEqualTo(randomAcctId);
              assertThat(account.getDouble("balance")).isEqualTo(10.00);
              tc.completeNow();
            }))
          );
      }

      @Test
      @DisplayName("You don't have any inconsistency between your write and read models")
      void a23(VertxTestContext tc) {
        readWebClient.get("/inconsistencies")
          .as(BodyCodec.jsonArray())
          .expect(ResponsePredicate.SC_SUCCESS)
          .send(tc.succeeding(response -> tc.verify(() -> {
              JsonArray result = response.body();
              assertThat(result.size()).isEqualTo(0);
              tc.completeNow();
            }))
          );
      }

      @Nested
      @DisplayName("When making a $5 withdraw on an account with balance = $10)")
      class When2 {

        @Test
        @DisplayName("You get version 2 and a $5 AmountWithdrawn event")
        void a3(VertxTestContext tc) {
          JsonObject cmdAsJson = new JsonObject("{\"amount\" : 5.00}");
          writeWebClient.post("/commands/account/" + randomAcctId + "/make-withdraw")
                  .as(BodyCodec.jsonObject())
                  .expect(ResponsePredicate.SC_SUCCESS)
                  .expect(ResponsePredicate.JSON)
                  .sendJson(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
                            JsonObject result = response.body();
                            Long uowId = new Long(response.getHeader("uowId"));
                            assertThat(uowId).isPositive();
                            assertThat(result.getString(ENTITY_NAME)).isEqualTo("account");
                            assertThat(result.getInteger(ENTITY_ID)).isEqualTo(randomAcctId);
                            assertThat(result.getString(COMMAND_ID)).isNotNull();
                            assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
                            assertThat(result.getInteger(VERSION)).isEqualTo(2);
                            assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(1);
                            assertThat(result.getJsonArray(EVENTS).getJsonObject(0).getDouble("amount"))
                                    .isEqualTo(5);
                            tc.completeNow();
                          }))
                  );
        }

        @DisplayName("You get account summary with balance = 5.00")
        void a4(VertxTestContext tc) throws InterruptedException {
          readWebClient.get("/commands/account/" + randomAcctId)
                  .as(BodyCodec.jsonObject())
                  .expect(ResponsePredicate.SC_SUCCESS)
                  .expect(ResponsePredicate.JSON)
                  .putHeader("accept", "application/json")
                  .send(tc.succeeding(response -> tc.verify(() -> {
                            JsonObject result = response.body();
                            assertThat(result.getInteger("id")).isEqualTo(randomAcctId);
                            assertThat(result.getDouble("balance")).isEqualTo(5.00);
                            tc.completeNow();
                          }))
                  );
        }

        @Nested
        @DisplayName("When making a $1 deposit on this account with balance = $5")
        class When3 {

          @Test
          @DisplayName("You get version 3 and and $1 AmountDeposit event")
          void a5(VertxTestContext tc) {
            JsonObject cmdAsJson = new JsonObject("{\"amount\" : 1.00}");
            writeWebClient.post("/commands/account/" + randomAcctId + "/make-deposit")
                    .as(BodyCodec.jsonObject())
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .expect(ResponsePredicate.JSON)
                    .sendJsonObject(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
                              JsonObject result = response.body();
                              Long uowId = new Long(response.getHeader("uowId"));
                              assertThat(uowId).isPositive();
                              assertThat(result.getString(ENTITY_NAME)).isEqualTo("account");
                              assertThat(result.getInteger(ENTITY_ID)).isEqualTo(randomAcctId);
                              assertThat(result.getString(COMMAND_ID)).isNotNull();
                              assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
                              assertThat(result.getInteger(VERSION)).isEqualTo(3);
                              assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(1);
                              assertThat(result.getJsonArray(EVENTS).getJsonObject(0).getDouble("amount"))
                                      .isEqualTo(1);
                              tc.completeNow();
                            }))
                    );
          }

          @DisplayName("You get account with just this account")
          void a6(VertxTestContext tc) throws InterruptedException {
            readWebClient.get("/commands/account")
                    .as(BodyCodec.jsonArray())
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .expect(ResponsePredicate.JSON)
                    .putHeader("accept", "application/json")
                    .send(tc.succeeding(response -> tc.verify(() -> {
                              JsonArray result = response.body();
                              assertThat(result.size()).isEqualTo(1);
                              JsonObject account = result.getJsonObject(0);
                              assertThat(account.getInteger("id")).isEqualTo(randomAcctId);
                              assertThat(account.getDouble("balance")).isEqualTo(6.00);
                              tc.completeNow();
                            }))
                    );
          }

          @DisplayName("You get account summary with balance = 6.00")
          void a7(VertxTestContext tc) throws InterruptedException {
            readWebClient.get("/commands/account/" + randomAcctId)
                    .as(BodyCodec.jsonObject())
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .expect(ResponsePredicate.JSON)
                    .putHeader("accept", "application/json")
                    .send(tc.succeeding(response -> tc.verify(() -> {
                              JsonObject result = response.body();
                              assertThat(result.getInteger("id")).isEqualTo(randomAcctId);
                              assertThat(result.getDouble("balance")).isEqualTo(6.00);
                              tc.completeNow();
                            }))
                    );
          }

          @Nested
          @DisplayName("When making a $100 withdraw on this account with balance = $6")
          class When4 {

            @Test
            @DisplayName("You get 400")
            void a3(VertxTestContext tc) {
              JsonObject cmdAsJson = new JsonObject("{\"amount\" : 100.00}");
              writeWebClient.post("/commands/account/" + randomAcctId + "/make-withdraw")
                      .as(BodyCodec.jsonObject())
                      .expect(ResponsePredicate.SC_BAD_REQUEST)
                      .sendJsonObject(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
                                JsonObject result = response.body();
                                assertThat(response.statusMessage()).isEqualTo("This account does not have enough balance");
                                assertThat(result).isNull();
                                tc.completeNow();
                              }))
                      );
            }

            @DisplayName("Your account summary keep with balance = 6.00")
            void a4(VertxTestContext tc) {
              readWebClient.get("/commands/account/" + randomAcctId)
                      .as(BodyCodec.jsonObject())
                      .expect(ResponsePredicate.SC_SUCCESS)
                      .expect(ResponsePredicate.JSON)
                      .putHeader("accept", "application/json")
                      .send(tc.succeeding(response -> tc.verify(() -> {
                                JsonObject result = response.body();
                                assertThat(result.getInteger("id")).isEqualTo(randomAcctId);
                                assertThat(result.getDouble("balance")).isEqualTo(6.00);
                                tc.completeNow();
                              }))
                      );
            }
          }
        }
      }
    }

  }

}