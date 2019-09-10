package com.crabzilla.examples.accounts.service.reports

import com.crabzilla.examples.accounts.model.AccountSummary
import com.crabzilla.examples.accounts.model.AccountsRepository
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple

/**
 * A read model repository to serve web routes.
 */
class AccountsRepositoryImpl(private val readModelDb: PgPool) : AccountsRepository {

  /**
   * Retrieves an account from read model given an ID
   */
  override fun accountById(accountId: Int, aHandler: Handler<AsyncResult<AccountSummary>>) {
    readModelDb.preparedQuery("select * from account_summary where id = $1", Tuple.of(accountId)) { event ->
      if (event.failed()) {
        aHandler.handle(Future.failedFuture(event.cause())); return@preparedQuery
      }
      val set = event.result()
      if (!set.iterator().hasNext()) {
        aHandler.handle(Future.succeededFuture(null)); return@preparedQuery
      }
      val row = set.iterator().next()
      val response = AccountSummary(row.getInteger("id"), row.getBigDecimal("balance"))
      aHandler.handle(Future.succeededFuture(response))
    }
  }

  /**
   * Retrieve all accounts from read model
   */
  override fun allAccounts(aHandler: Handler<AsyncResult<List<AccountSummary>>>) {
    readModelDb.preparedQuery("select * from account_summary order by id") { event ->
      if (event.failed()) {
        aHandler.handle(Future.failedFuture(event.cause())); return@preparedQuery
      }
      val set = event.result()
      val array = ArrayList<AccountSummary>()
      for (row in set) {
        array.add(AccountSummary(row.getInteger("id"), row.getBigDecimal("balance")))
      }
      aHandler.handle(Future.succeededFuture(array))
    }
  }

}