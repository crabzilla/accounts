package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.examples.accounts.domain.AccountCreated
import io.github.crabzilla.examples.accounts.domain.AmountDeposited
import io.github.crabzilla.examples.accounts.domain.AmountWithdrawn
import io.github.crabzilla.examples.accounts.domain.accountsModule
import io.github.crabzilla.examples.accounts.infra.boilerplate.addSingletonListener
import io.github.crabzilla.examples.accounts.infra.boilerplate.readModelPgPool
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcReadContext
import io.github.crabzilla.pgc.addProjector
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DbProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DbProjectionsVerticle::class.java)
  }

  private val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }

  override fun start(startPromise: Promise<Void>) {

    addSingletonListener(vertx, this::class.java.name)

    val accountsJson = Json(context = accountsModule)
    val readContext = PgcReadContext(vertx, accountsJson, readDb)
    addProjector(readContext, "accounts-summary", AccountsSummaryProjector())

    startPromise.complete()
  }

  private class AccountsSummaryProjector : PgcEventProjector {
    override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
      return when (event) {
        is AccountCreated -> {
          val query = "INSERT INTO account_summary (id) VALUES ($1)"
          val tuple = Tuple.of(targetId)
          executePreparedQuery(pgTx, query, tuple)
        }
        is AmountDeposited -> {
          val query = "UPDATE account_summary SET balance = balance + $1 WHERE id = $2"
          val tuple = Tuple.of(event.amount, targetId)
          executePreparedQuery(pgTx, query, tuple)
        }
        is AmountWithdrawn -> {
          val query = "UPDATE account_summary SET balance = balance - $1 WHERE id = $2"
          val tuple = Tuple.of(event.amount, targetId)
          executePreparedQuery(pgTx, query, tuple)
        }
        else -> {
          return Future.succeededFuture()
        }
      }
    }
  }
}
