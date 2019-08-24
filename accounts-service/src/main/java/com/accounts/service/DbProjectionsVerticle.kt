package com.accounts.service

import com.accounts.model.AccountCreated
import com.accounts.model.AmountDeposited
import com.accounts.model.AmountWithdrawn
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.pgc.PgcDbProjectionsVerticle
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future

class DbProjectionsVerticle : PgcDbProjectionsVerticle() {

  override fun start(startFuture: Future<Void>) {
    super.start(startFuture)
    addProjector("accounts-summary", AccountsSummaryProjector())
    startFuture.complete()
  }

}

class AccountsSummaryProjector : PgcEventProjector {

  override fun handle(pgTx: PgTransaction, targetId: Int, event: DomainEvent): Future<Void> {
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