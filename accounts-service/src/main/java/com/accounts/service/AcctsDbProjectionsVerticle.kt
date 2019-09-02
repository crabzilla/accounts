package com.accounts.service

import com.accounts.model.AccountCreated
import com.accounts.model.AccountJsonAware
import com.accounts.model.AmountDeposited
import com.accounts.model.AmountWithdrawn
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future
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

  override fun handle(pgTx: PgTransaction, targetId: Int, event: DomainEvent): Future<Void> {
    log.info("Will project event $event")
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