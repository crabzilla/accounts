package com.accounts.service;

import com.accounts.model.MakeDeposit;
import com.accounts.model.MakeWithdraw;
import io.reactiverse.pgclient.PgPool;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
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

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.Random;

import static io.github.crabzilla.CrabzillaKt.initCrabzilla;
import static io.github.crabzilla.UnitOfWork.JsonMetadata.*;
import static io.github.crabzilla.pgc.PgcKt.readModelPgPool;
import static io.github.crabzilla.pgc.PgcKt.writeModelPgPool;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Testing success scenarios
**/
@ExtendWith(VertxExtension.class)
class AcceptanceIT {

  private static final Logger log = LoggerFactory.getLogger(AcceptanceIT.class);
  private static WebClient client;
  private static int port;
  private static final Random random = new Random();
  private static int randomAcctId = random.nextInt();

  static {
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
            SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory.class);// Required for Logback to work in Vertx
  }

  private static int httpPort() {
    int httpPort = 0;
    try {
      ServerSocket socket = new ServerSocket(0);
      httpPort = socket.getLocalPort();
      socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return httpPort;
  }

  private static ConfigRetriever configRetriever(Vertx vertx, String configFile) {
    ConfigStoreOptions envOptions = new ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(new JsonObject().put("path", configFile));
    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(envOptions);
    return ConfigRetriever.create(vertx, options);
  }

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    initCrabzilla(vertx);
    port = httpPort();
    configRetriever(vertx, "./../accounts.env").getConfig(gotConfig -> {
      if (gotConfig.succeeded()) {
        JsonObject config = gotConfig.result();
        config.put("HTTP_PORT", port);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config);
        WebClientOptions wco = new WebClientOptions();
        client = WebClient.create(vertx, wco);
        vertx.deployVerticle(WebRoutesVerticle.class, deploymentOptions, deploy1 -> {
          if (deploy1.succeeded()) {
            vertx.deployVerticle(DbProjectionsVerticle.class, deploymentOptions, deploy2 -> {
              if (deploy2.succeeded()) {
                PgPool read = readModelPgPool(vertx, config);
                PgPool write = writeModelPgPool(vertx, config);
                write.query("delete from units_of_work", event1 -> {
                  if (event1.failed()) {
                    tc.failNow(event1.cause());
                    return;
                  }
                  write.query("delete from account_snapshots", event2 -> {
                    if (event2.failed()) {
                      tc.failNow(event2.cause());
                      return;
                    }
                    read.query("delete from account_summary", event3 -> {
                      if (event3.failed()) {
                        tc.failNow(event3.cause());
                        return;
                      }
                      tc.completeNow();
                    });
                  });
                });
              } else {
                tc.failNow(deploy2.cause());
              }
            });
          } else {
            tc.failNow(deploy1.cause());
          }
        });
      } else {
        tc.failNow(gotConfig.cause());
      }
    });

  }

  @Nested
  @DisplayName("When making a $10 withdraw on a missing account")
  class When5 {
    @Test
    @DisplayName("You get a bad request")
    void a7(VertxTestContext tc) {
      MakeWithdraw makeWithdraw = new MakeWithdraw(new BigDecimal(10));
      JsonObject cmdAsJson = JsonObject.mapFrom(makeWithdraw);
      client.post(port, "0.0.0.0", "/accounts/" + random.nextInt() + "/commands/make-withdraw")
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_BAD_REQUEST)
        .sendJsonObject(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
            assertThat(response.body()).isNull();
            assertThat(response.statusMessage()).isEqualTo("This account must exists");
            tc.completeNow();
          }))
        );
    }
  }

  @Nested
  @DisplayName("When making a $10 deposit on a new account")
  class When1 {

    @Test
    @Order(1)
    @DisplayName("You get version 1 and events = AccountCreated and $10 AmountDeposit")
    void a1(VertxTestContext tc) {
      MakeDeposit makeDeposit = new MakeDeposit(new BigDecimal(10));
      JsonObject cmdAsJson = JsonObject.mapFrom(makeDeposit);
      client.post(port, "0.0.0.0", "/accounts/" + randomAcctId + "/commands/make-deposit")
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJsonObject(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
          JsonObject result = response.body();
          System.out.println(result.encodePrettily());
          Long uowId = new Long(response.getHeader("uowId"));
          assertThat(uowId).isPositive();
          assertThat(result.getString(ENTITY_NAME)).isEqualTo("account");
          assertThat(result.getInteger(ENTITY_ID)).isEqualTo(randomAcctId);
          assertThat(result.getString(COMMAND_ID)).isNotNull();
          assertThat(result.getString(COMMAND_NAME)).isEqualTo("make-deposit");
          assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
          assertThat(result.getInteger(VERSION)).isEqualTo(1);
          assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(2);
          assertThat(result.getJsonArray(EVENTS).getJsonObject(0).getJsonObject("second")
                  .getJsonObject("accountId").getDouble("value")).isEqualTo(randomAcctId);
          assertThat(result.getJsonArray(EVENTS).getJsonObject(1).getJsonObject("second")
                  .getDouble("amount")).isEqualTo(makeDeposit.component1().intValue());
          tc.completeNow();
        }))
      );
    }

    @Test
    @Order(2)
    @DisplayName("You get account summary with balance = 10.00")
    void a2(VertxTestContext tc) {
      
      client.get(port, "0.0.0.0", "/accounts/" + randomAcctId)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .putHeader("accept", "application/json")
        .send(tc.succeeding(response -> tc.verify(() -> {
          JsonObject result = response.body();
          System.out.println(result.encodePrettily());
          assertThat(result.getInteger("accountId")).isEqualTo(randomAcctId);
          assertThat(result.getDouble("balance")).isEqualTo(10.00);
          tc.completeNow();
        }))
      );
    }

    @Test
    @Order(3)
    @DisplayName("You get accounts with just this account")
    void a22(VertxTestContext tc) {
      
      client.get(port, "0.0.0.0", "/accounts")
        .as(BodyCodec.jsonArray())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
//        .putHeader("accept", "application/json")
        .send(tc.succeeding(response -> tc.verify(() -> {
            JsonArray result = response.body();
            assertThat(result.size()).isEqualTo(1);
            JsonObject account = result.getJsonObject(0);
            assertThat(account.getInteger("accountId")).isEqualTo(randomAcctId);
            assertThat(account.getDouble("balance")).isEqualTo(10.00);
            tc.completeNow();
          }))
        );
    }

    @Order(4)
    @Test
    @DisplayName("You don't have any inconsistency between your write and read models")
    void a23(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/inconsistencies")
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_NOT_FOUND)
        .putHeader("accept", "application/json")
        .send(tc.succeeding(response -> tc.verify(() -> {
            String result = response.body();
            assertThat(result).isEqualTo("Both write and read models seems to be consistent, yay!");
            tc.completeNow();
          }))
        );
    }

    @Nested
    @DisplayName("When making a $5 withdraw on an account with balance = $10)")
    class When2 {

      @Test
      @Order(1)
      @DisplayName("You get version 2 and a $5 AmountWithdrawn event")
      void a3(VertxTestContext tc) {
        MakeWithdraw makeWithdraw = new MakeWithdraw(new BigDecimal(5));
        JsonObject cmdAsJson = JsonObject.mapFrom(makeWithdraw);
        client.post(port, "0.0.0.0", "/accounts/" + randomAcctId + "/commands/make-withdraw")
          .as(BodyCodec.jsonObject())
          .expect(ResponsePredicate.SC_SUCCESS)
          .expect(ResponsePredicate.JSON)
          .sendJson(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
            JsonObject result = response.body();
            System.out.println(result.encodePrettily());
            Long uowId = new Long(response.getHeader("uowId"));
            assertThat(uowId).isPositive();
            assertThat(result.getString(ENTITY_NAME)).isEqualTo("account");
            assertThat(result.getInteger(ENTITY_ID)).isEqualTo(randomAcctId);
            assertThat(result.getString(COMMAND_ID)).isNotNull();
            assertThat(result.getString(COMMAND_NAME)).isEqualTo("make-withdraw");
            assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
            assertThat(result.getInteger(VERSION)).isEqualTo(2);
            assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(1);
            assertThat(result.getJsonArray(EVENTS).getJsonObject(0).getJsonObject("second").getDouble("amount"))
                    .isEqualTo(makeWithdraw.component1().intValue());
            tc.completeNow();
          }))
        );
      }

      @Order(2)
      @DisplayName("You get account summary with balance = 5.00")
      void a4(VertxTestContext tc) throws InterruptedException {
        
        client.get(port, "0.0.0.0", "/accounts/" + randomAcctId)
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
        @Order(1)
        @DisplayName("You get version 3 and and $1 AmountDeposit event")
        void a5(VertxTestContext tc) {
          MakeDeposit makeDeposit = new MakeDeposit(new BigDecimal(1));
          JsonObject cmdAsJson = JsonObject.mapFrom(makeDeposit);
          client.post(port, "0.0.0.0", "/accounts/" + randomAcctId + "/commands/make-deposit")
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
              assertThat(result.getString(COMMAND_NAME)).isEqualTo("make-deposit");
              assertThat(result.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
              assertThat(result.getInteger(VERSION)).isEqualTo(3);
              assertThat(result.getJsonArray(EVENTS).size()).isEqualTo(1);
              assertThat(result.getJsonArray(EVENTS).getJsonObject(0).getJsonObject("second").getDouble("amount"))
                      .isEqualTo(makeDeposit.component1().intValue());
              tc.completeNow();
            }))
          );
        }

        @Order(2)
        @DisplayName("You get accounts with just this account")
        void a6(VertxTestContext tc) throws InterruptedException {
          Thread.sleep(500);
          client.get(port, "0.0.0.0", "/accounts")
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

        @Order(3)
        @DisplayName("You get account summary with balance = 6.00")
        void a7(VertxTestContext tc) throws InterruptedException {
          
          client.get(port, "0.0.0.0", "/accounts/" + randomAcctId)
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
          @Order(1)
          @DisplayName("You get 400")
          void a3(VertxTestContext tc) {
            MakeWithdraw makeWithdraw = new MakeWithdraw(new BigDecimal(100));
            JsonObject cmdAsJson = JsonObject.mapFrom(makeWithdraw);
            client.post(port, "0.0.0.0", "/accounts/" + randomAcctId + "/commands/make-withdraw")
              .as(BodyCodec.jsonObject())
              .expect(ResponsePredicate.SC_BAD_REQUEST)
              .sendJson(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
                JsonObject result = response.body();
                assertThat(response.statusMessage()).isEqualTo("This account does not have enough balance");
                assertThat(result).isNull();
                tc.completeNow();
              }))
            );
          }

          @Order(2)
          @DisplayName("Your account summary keep with balance = 6.00")
          void a4(VertxTestContext tc) {
            
            client.get(port, "0.0.0.0", "/accounts/" + randomAcctId)
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