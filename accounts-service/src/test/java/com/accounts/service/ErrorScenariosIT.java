package com.accounts.service;

import com.accounts.model.MakeDeposit;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.Random;

import static io.github.crabzilla.CrabzillaKt.initCrabzilla;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Testing error scenarios
**/
@ExtendWith(VertxExtension.class)
class ErrorScenariosIT {

  private static final Random random = new Random();
  private static final Logger log = LoggerFactory.getLogger(ErrorScenariosIT.class);
  private static WebClient client;
  private static int port;

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


  static private ConfigRetriever configRetriever(Vertx vertx, String configFile) {
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
        vertx.deployVerticle(AccountsVerticle.class, deploymentOptions, deploy -> {
          if (deploy.succeeded()) {
            tc.completeNow();
          } else {
            deploy.cause().printStackTrace();
            tc.failNow(deploy.cause());
          }
        });
      } else {
        tc.failNow(gotConfig.cause());
      }
    });

  }

  @Nested
  @DisplayName("When GET to on a missing account")
  class When6 {

    @Test
    @DisplayName("You get a 404")
    void a7(VertxTestContext tc) {
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
  class When7 {

    @Test
    @DisplayName("You get a 400")
    void a7(VertxTestContext tc) {
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
  class When8 {
    @Test
    @DisplayName("You get a 400")
    void a1(VertxTestContext tc) {
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
  class When9 {
    @Test
    @DisplayName("You get a 400")
    void a1(VertxTestContext tc) {
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