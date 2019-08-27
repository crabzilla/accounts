package com.accounts.service

import com.accounts.model.AccountCmdAware
import com.accounts.model.AccountJsonAware
import io.github.crabzilla.webpgc.WebCommandVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

class AcctsWebCommandVerticle : WebCommandVerticle() {

  companion object {
    internal val log = getLogger(AcctsWebCommandVerticle::class.java)
  }

  private lateinit var server: HttpServer

  override fun start(future: Future<Void>) {

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // command routes
    addResourceForEntity("accounts", "account", AccountJsonAware(), AccountCmdAware(), router)

    // http server
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

}

