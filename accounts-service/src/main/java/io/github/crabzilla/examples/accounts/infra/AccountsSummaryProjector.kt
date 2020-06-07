package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.examples.accounts.domain.AccountCreated
import io.github.crabzilla.examples.accounts.domain.AmountDeposited
import io.github.crabzilla.examples.accounts.domain.AmountWithdrawn
import io.github.crabzilla.pgc.query.PgcDomainEventProjector
import io.vertx.core.Future
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

class AccountsSummaryProjector : PgcDomainEventProjector {
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
