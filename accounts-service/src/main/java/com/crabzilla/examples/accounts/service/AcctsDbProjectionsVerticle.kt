package com.crabzilla.examples.accounts.service

import com.crabzilla.examples.accounts.model.AccountCreated
import com.crabzilla.examples.accounts.model.AccountJsonAware
import com.crabzilla.examples.accounts.model.AmountDeposited
import com.crabzilla.examples.accounts.model.AmountWithdrawn
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Future
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class AcctsDbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(startFuture: Future<Void>) {
    super.start()
    addEntityJsonAware("account", AccountJsonAware())
    addProjector("accounts-summary", AccountsSummaryProjector())
    startFuture.complete()
  }

}

private class AccountsSummaryProjector : PgcEventProjector {

  private val log: Logger = LoggerFactory.getLogger(Main::class.java)

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    val future: Future<Void> = Future.future()
    when (event) {
      is AccountCreated -> {
        val query = "INSERT INTO account_summary (id) VALUES ($1)"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      is AmountDeposited -> {
        val query = "UPDATE account_summary SET balance = balance + $1 WHERE id = $2"
        val tuple = Tuple.of(event.amount, targetId)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      is AmountWithdrawn -> {
        val query = "UPDATE account_summary SET balance = balance - $1 WHERE id = $2"
        val tuple = Tuple.of(event.amount, targetId)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      else -> {
        future.complete()
      }
    }
    return future
  }

}