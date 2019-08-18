package com.accounts.service

import com.accounts.model.AccountCmdAware
import com.accounts.model.AccountJsonAware
import com.accounts.service.model.read.AccountsRepositoryImpl
import com.accounts.service.model.read.AccountsWebHandlers
import io.github.crabzilla.pgc.PgcComponent
import io.github.crabzilla.webpgc.WebPgcCmdHandlerComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

class AccountsVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(AccountsVerticle::class.java)
  }

  private lateinit var pgcComponent : PgcComponent

  override fun start(future: Future<Void>) {

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // command routes
    pgcComponent = PgcComponent(vertx, config)
    val cmdHandlerComponent = WebPgcCmdHandlerComponent(pgcComponent, router)
    cmdHandlerComponent.addCommandHandler("account", AccountJsonAware(), AccountCmdAware(), "accounts")

    // reports routes
    val accountHandlers = AccountsWebHandlers(AccountsRepositoryImpl(pgcComponent.readDb))
    router.get("/accounts/:id").handler { rc -> accountHandlers.account(rc) }
    router.get("/accounts").handler { rc -> accountHandlers.allAccounts(rc) }
    val consistencyHandler = ConsistencyWebHandlers(ConsistencyRepository(pgcComponent.readDb))
    router.get("/inconsistencies").handler { rc -> consistencyHandler.inconsistencies(rc) }

    // http server
    val httpPort = config.getInteger("HTTP_PORT")
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
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
    pgcComponent.writeDb.close()
    pgcComponent.readDb.close()
    future.complete()
  }

}

