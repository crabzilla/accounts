package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.examples.accounts.domain.accountsModule
import io.github.crabzilla.examples.accounts.infra.boilerplate.PgClientSupport.readModelPgPool
import io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.SingletonClusteredVerticle
import io.github.crabzilla.pgc.query.PgcReadContext
import io.github.crabzilla.pgc.query.startStreamProjectionBroker
import io.github.crabzilla.pgc.query.startStreamProjectionConsumer
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventbusProjectionsVerticle : AbstractVerticle(), SingletonClusteredVerticle {

  companion object {
    val log: Logger = LoggerFactory.getLogger(EventbusProjectionsVerticle::class.java)
  }

  private val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }

  override fun logger(): Logger {
    return log
  }

  override fun start(startPromise: Promise<Void>) {

    addSingletonListener()

    val accountsJson = Json(context = accountsModule)
    val readContext = PgcReadContext(vertx, accountsJson, readDb)

    startStreamProjectionBroker(vertx, "account", listOf("accounts-summary"))
    startStreamProjectionConsumer("account", "accounts-summary", readContext, AccountsSummaryProjector())

    startPromise.complete()
  }

  override fun stop(promise: Promise<Void>) {
    log.info("Closing resources")
    readDb.close()
    promise.complete()
  }

}
