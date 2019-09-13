package com.crabzilla.examples.accounts.service

import com.crabzilla.examples.accounts.model.AccountCreated
import com.crabzilla.examples.accounts.model.AccountJsonAware
import com.crabzilla.examples.accounts.model.AmountDeposited
import com.crabzilla.examples.accounts.model.AmountWithdrawn
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Promise
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

open class AcctsDbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(startPromise: Promise<Void>) {
    super.start()
    addEntityJsonAware("account", AccountJsonAware())
    addProjector("accounts-summary", AccountsSummaryProjector())
    startPromise.complete()
  }

}

private class AccountsSummaryProjector : PgcEventProjector {

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Promise<Void> {
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
        Promise.failedPromise<Void>("Unknown event ${event.javaClass.name}")
      }
    }
  }

}