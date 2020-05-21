package io.github.crabzilla.examples.accounts.infra;

import io.github.crabzilla.examples.accounts.domain.MakeDeposit;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static io.github.crabzilla.examples.accounts.infra.boilerplate.CleanDatabaseKt.cleanDatabase;
import static io.vertx.junit5.web.TestRequest.*;

/**
 * Testing error scenarios
 **/
@ExtendWith(VertxExtension.class)
class ErrorScenariosIT {

  private static final Logger log = LoggerFactory.getLogger(ErrorScenariosIT.class);
  private static final Random random = new Random();
  private static WebClient readWebClient;
  private static WebClient writeWebClient;

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.getConfig(vertx, "./../accounts.env")
      .onFailure(tc::failNow)
      .onSuccess(config -> {
        config.put("WRITE_HTTP_PORT", io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.findFreeHttpPort());
        config.put("READ_HTTP_PORT", io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.findFreeHttpPort()+1);
        writeWebClient = create(vertx, config.getInteger("WRITE_HTTP_PORT"));
        readWebClient = create(vertx, config.getInteger("READ_HTTP_PORT"));
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config).setInstances(1);
        CompositeFuture.all(
          io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.deploy(vertx, WebCommandVerticle.class.getName(), deploymentOptions),
          io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.deploy(vertx, WebQueryVerticle.class.getName(), deploymentOptions),
          io.github.crabzilla.examples.accounts.infra.boilerplate.Web_boilerplateKt.deploy(vertx, DbProjectionsVerticle.class.getName(), deploymentOptions)
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
  @DisplayName("When GET to on a missing account")
  class When11 {

    @Test
    @DisplayName("You get a 404")
    void a11(VertxTestContext tc) {
      testRequest(readWebClient, HttpMethod.GET, "/accounts/" + random.nextInt())
        .expect(statusCode(404))
        .expect(statusMessage("Not Found"))
        .send(tc);
    }

  }

  @Nested
  @DisplayName("When GET to on a invalid account (ID not a number)")
  class When12 {

    @Test
    @DisplayName("You get a 400")
    void a12(VertxTestContext tc) {
      testRequest(readWebClient, HttpMethod.GET, "/accounts/ddd")
        .expect(statusCode(500))
        .send(tc);
    }
  }

  @Nested
  @DisplayName("When making a $10 deposit on an invalid account (ID not a number)")
  class When13 {
    @Test
    @DisplayName("You get a 404")
    void a13(VertxTestContext tc) {
      MakeDeposit makeDeposit = new MakeDeposit(10);
      JsonObject cmdAsJson = JsonObject.mapFrom(makeDeposit);
      testRequest(writeWebClient, HttpMethod.POST, "/commands/accounts/NOT_A_NUMBER/make-deposit")
        .expect(statusCode(404))
        .expect(statusMessage("Not Found"))
        .sendJson(cmdAsJson, tc);
    }
  }

  @Nested
  @DisplayName("When GET to an invalid UnitOfWork (bad number)")
  class When14 {
    @Test
    @DisplayName("You get a 404")
    void a14(VertxTestContext tc) {
      testRequest(readWebClient, HttpMethod.GET, "/commands/accounts/units-of-work/dddd")
        .expect(statusCode(404))
        .expect(statusMessage("Not Found"))
        .send(tc);
    }
  }

}