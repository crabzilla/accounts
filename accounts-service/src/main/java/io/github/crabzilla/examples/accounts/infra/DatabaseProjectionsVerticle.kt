package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.examples.accounts.domain.accountsModule
import io.github.crabzilla.examples.accounts.infra.boilerplate.PgClientSupport.readModelPgPool
import io.github.crabzilla.examples.accounts.infra.boilerplate.PgClientSupport.writeModelPgPool
import io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.SingletonClusteredVerticle
import io.github.crabzilla.pgc.PgcStreamProjector
import io.github.crabzilla.pgc.query.PgcProjectionsRepo
import io.github.crabzilla.pgc.query.PgcReadContext
import io.github.crabzilla.pgc.query.startStreamProjectionConsumer
import io.github.crabzilla.pgc.query.startStreamProjectionDbPoolingProducer
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DatabaseProjectionsVerticle : AbstractVerticle(), SingletonClusteredVerticle {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DatabaseProjectionsVerticle::class.java)
  }

  private val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  private val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }

  lateinit var server: HttpServer

  override fun logger(): Logger {
    return log
  }

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    addSingletonListener()

    val accountsJson = Json(context = accountsModule)

    // projection consumer
    val readContext = PgcReadContext(vertx, accountsJson, readDb)
    startStreamProjectionConsumer("account", "accounts-summary",
      readContext, AccountsSummaryProjector())

    // projection producer
    val streamProjector = PgcStreamProjector(vertx, writeDb, "account", "accounts-summary")
    startStreamProjectionDbPoolingProducer(vertx, PgcProjectionsRepo(readDb), streamProjector)
      .onFailure { err -> promise.fail(err) }
      .onSuccess { promise.complete()}

  }

  override fun stop(promise: Promise<Void>) {
    readDb.close()
    promise.complete()
  }

}
