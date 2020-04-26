package com.crabzilla.examples.accounts.infra

import com.crabzilla.examples.accounts.domain.AccountCreated
import com.crabzilla.examples.accounts.domain.AmountDeposited
import com.crabzilla.examples.accounts.domain.AmountWithdrawn
import com.crabzilla.examples.accounts.domain.accountsJson
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

open class AcctsDbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(startPromise: Promise<Void>) {
    super.start()
    addProjector("accounts-summary", AccountsSummaryProjector(), accountsJson)
    startPromise.complete()
  }
}

private class AccountsSummaryProjector : PgcEventProjector {

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    return when (event) {
      is AccountCreated -> {
        val query = "INSERT INTO account_summary (id) VALUES ($1)"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple)
      }
      is AmountDeposited -> {
        val query = "UPDATE account_summary SET balance = balance + $1 WHERE id = $2"
        val tuple = Tuple.of(event.amount, targetId)
        pgTx.runPreparedQuery(query, tuple)
      }
      is AmountWithdrawn -> {
        val query = "UPDATE account_summary SET balance = balance - $1 WHERE id = $2"
        val tuple = Tuple.of(event.amount, targetId)
        pgTx.runPreparedQuery(query, tuple)
      }
      else -> {
        return Future.succeededFuture()
      }
    }
  }
}
