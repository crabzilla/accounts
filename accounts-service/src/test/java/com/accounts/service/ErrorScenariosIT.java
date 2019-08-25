package com.accounts.service;

import com.accounts.model.MakeDeposit;
import io.github.crabzilla.webpgc.DeploymentConventions;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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

import static io.github.crabzilla.pgc.PgcKt.readModelPgPool;
import static io.github.crabzilla.pgc.PgcKt.writeModelPgPool;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Testing error scenarios
**/
@ExtendWith(VertxExtension.class)
class ErrorScenariosIT {

  static {
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
            SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory.class);// Required for Logback to work in Vertx
  }

  private static final Logger log = LoggerFactory.getLogger(ErrorScenariosIT.class);
  private static final Random random = new Random();
  private static WebClient client;

  private static int port = findFreeHttpPort();
  private static int findFreeHttpPort() {
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

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    DeploymentConventions.INSTANCE.getConfig(vertx,  "./../accounts.env")
      .setHandler(gotConfig -> {
        if (gotConfig.failed()) {
          tc.failNow(gotConfig.cause());
          return;
        }
        JsonObject config = gotConfig.result();
        config.put("HTTP_PORT", port);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config).setInstances(1);
        WebClientOptions wco = new WebClientOptions();
        client = WebClient.create(vertx, wco);
        CompositeFuture.all(
          DeploymentConventions.INSTANCE.deploy(vertx, AcctsWebVerticle.class.getName(), deploymentOptions),
          DeploymentConventions.INSTANCE.deploy(vertx, AccountsDbPjcVerticle.class.getName(), deploymentOptions))
          .setHandler(deploy ->  {
            if (deploy.succeeded()) {
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
              deploy.cause().printStackTrace();
              tc.failNow(deploy.cause());
            }
          }
        );
      }
    );
  }

  @Nested
  @DisplayName("When GET to on a missing account")
  class When11 {

    @Test
    @DisplayName("You get a 404")
    void a11(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/accounts/" + random.nextInt())
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_NOT_FOUND)
        .putHeader("accept", "application/json")
        .send(tc.succeeding(response -> tc.verify(() -> {
          String result = response.body();
          assertThat(result).isEqualTo("Account not found");
          tc.completeNow();
        }))
      );
    }

  }

  @Nested
  @DisplayName("When GET to on a invalid account (ID not a number)")
  class When12 {

    @Test
    @DisplayName("You get a 400")
    void a12(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/accounts/dd")
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_BAD_REQUEST)
        .send(tc.succeeding(response -> tc.verify(() -> {
            String result = response.body();
            assertThat(result).isEqualTo("path param entityId must be a number");
            tc.completeNow();
        }))
      );
    }
  }

  @Nested
  @DisplayName("When making a $10 deposit on an invalid account (ID not a number)")
  class When13 {
    @Test
    @DisplayName("You get a 400")
    void a13(VertxTestContext tc) {
      MakeDeposit makeDeposit = new MakeDeposit(new BigDecimal(1));
      JsonObject cmdAsJson = JsonObject.mapFrom(makeDeposit);
      client.post(port, "0.0.0.0", "/accounts/NOT_A_NUMBER/commands/make-deposit")
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_BAD_REQUEST)
        .sendJsonObject(cmdAsJson, tc.succeeding(response -> tc.verify(() -> {
          String result = response.body();
          assertThat(result).isEqualTo("path param entityId must be a number");
          tc.completeNow();
        }))
      );
    }
  }

  @Nested
  @DisplayName("When GET to an invalid UnitOfWork (bad number)")
  class When14 {
    @Test
    @DisplayName("You get a 400")
    void a14(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/accounts/units-of-work/dddd")
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_BAD_REQUEST)
        .send(tc.succeeding(response -> tc.verify(() -> {
            String result = response.body();
            assertThat(result).isEqualTo("path param unitOfWorkId must be a number");
            tc.completeNow();
        }))
      );
    }
  }

}