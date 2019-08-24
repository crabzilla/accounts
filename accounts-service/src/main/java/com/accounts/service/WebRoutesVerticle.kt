package com.accounts.service

import com.accounts.model.AccountCmdAware
import com.accounts.model.AccountJsonAware
import com.accounts.service.reports.AccountsRepositoryImpl
import com.accounts.service.reports.AccountsWebHandlers
import com.accounts.service.reports.ConsistencyRepository
import com.accounts.service.reports.ConsistencyWebHandlers
import io.github.crabzilla.pgc.PgcComponent
import io.github.crabzilla.webpgc.WebPgcCmdHandlerComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.slf4j.LoggerFactory.getLogger

class WebRoutesVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(WebRoutesVerticle::class.java)
  }

  private lateinit var server: HttpServer
  private lateinit var pgcComponent : PgcComponent

  override fun start(future: Future<Void>) {

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // command routes
    pgcComponent = PgcComponent(vertx, config)
    val webCmdHandlerComponent = WebPgcCmdHandlerComponent(pgcComponent, router)
    webCmdHandlerComponent.addCommandHandler("account", AccountJsonAware(), AccountCmdAware(), "accounts")

    // reports routes
    val accountHandlers = AccountsWebHandlers(AccountsRepositoryImpl(pgcComponent.readDb))
    router.get("/accounts/:id").handler { rc -> accountHandlers.account(rc) }
    router.get("/accounts").handler { rc -> accountHandlers.allAccounts(rc) }
    val consistencyHandler = ConsistencyWebHandlers(ConsistencyRepository(pgcComponent.readDb))
    router.get("/inconsistencies").handler { rc -> consistencyHandler.inconsistencies(rc) }

    // eventBus to browser
    val sockJSHandler = SockJSHandler.create(vertx)
    // Allow events for the designated addresses in/out of the event bus bridge
    val options = BridgeOptions()
            .addOutboundPermitted(PermittedOptions().setAddress(config.getString("UI_PROJECTION_ENDPOINT")))
    sockJSHandler.bridge(options)
    router.route("/eventbus/*").handler(sockJSHandler)

    // Serve the static resources
    router.route().handler(StaticHandler.create().setIndexPage("index.html").setWebRoot("webroot"));

    // http server
    val httpPort = config.getInteger("HTTP_PORT")
    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen { startedFuture ->
      if (startedFuture.succeeded()) {
        log.info("Server started on port " + startedFuture.result().actualPort())
        future.complete()
      } else {
        log.error("oops, something went wrong during server initialization", startedFuture.cause())
        future.fail(startedFuture.cause())
      }
    }

  }

  override fun stop(future: Future<Void>) {
    log.info("*** stopping")
    server.close()
    pgcComponent.writeDb.close()
    pgcComponent.readDb.close()
    future.complete()
  }

}
