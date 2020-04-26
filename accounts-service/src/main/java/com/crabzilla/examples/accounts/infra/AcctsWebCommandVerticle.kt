package com.crabzilla.examples.accounts.infra

import com.crabzilla.examples.accounts.domain.AccountCmdAware
import com.crabzilla.examples.accounts.domain.MakeDeposit
import com.crabzilla.examples.accounts.domain.MakeWithdraw
import com.crabzilla.examples.accounts.domain.accountsJson
import io.github.crabzilla.webpgc.WebCommandVerticle
import io.github.crabzilla.webpgc.listenHandler
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

class AcctsWebCommandVerticle : WebCommandVerticle() {

  companion object {
    internal val log = getLogger(AcctsWebCommandVerticle::class.java)
  }

  override fun start(promise: Promise<Void>) {

    log.info("*** httpPort: $httpPort")

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    val cmdTypeMap = mapOf(
            Pair("make-deposit", MakeDeposit::class.qualifiedName as String),
            Pair("make-withdraw", MakeWithdraw::class.qualifiedName as String))

    // command routes
    addResourceForEntity("accounts", "account", AccountCmdAware(), cmdTypeMap, accountsJson, router)

    // http server
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(promise))
  }
}
