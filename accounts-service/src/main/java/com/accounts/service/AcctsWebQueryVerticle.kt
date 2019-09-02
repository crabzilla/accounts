package com.accounts.service

import com.accounts.service.reports.AccountsRepositoryImpl
import com.accounts.service.reports.AccountsWebHandlers
import com.accounts.service.reports.ConsistencyRepository
import com.accounts.service.reports.ConsistencyWebHandlers
import io.github.crabzilla.webpgc.WebQueryVerticle
import io.github.crabzilla.webpgc.listenHandler
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

class AcctsWebQueryVerticle : WebQueryVerticle() {

  companion object {
    internal val log = getLogger(AcctsWebQueryVerticle::class.java)
  }

  override fun start(future: Future<Void>) {

    log.info("*** httpPort: $httpPort")

    val config = config()

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // reports routes
    val accountHandlers = AccountsWebHandlers(AccountsRepositoryImpl(readDb))
    router.get("/accounts/:id").handler { rc -> accountHandlers.account(rc) }
    router.get("/accounts").handler { rc -> accountHandlers.allAccounts(rc) }
    val consistencyHandler = ConsistencyWebHandlers(ConsistencyRepository(readDb))
    router.get("/inconsistencies").handler { rc -> consistencyHandler.inconsistencies(rc) }

    // eventBus to browser
    val sockJSHandler = SockJSHandler.create(vertx)
    // Allow events for the designated addresses in/out of the event bus bridge
    val options = BridgeOptions().addOutboundPermitted(PermittedOptions()
                                 .setAddress(config.getString("UI_PROJECTION_CHANNEL")))
    sockJSHandler.bridge(options)
    router.route("/eventbus/*").handler(sockJSHandler)

    // Serve the static resources
    router.route().handler(StaticHandler.create().setIndexPage("index.html").setWebRoot("webroot"));

    // http server
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(future))

  }

}

