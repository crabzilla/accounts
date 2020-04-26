package com.crabzilla.examples.accounts.infra.reports

import com.crabzilla.examples.accounts.domain.AccountBalance
import com.crabzilla.examples.accounts.domain.AccountsRepository
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple

/**
 * A read model repository to serve web routes.
 */
class AccountsRepositoryImpl(private val readModelDb: PgPool) : AccountsRepository {

  /**
   * Retrieves an account from read model given an ID
   */
  override fun accountById(accountId: Int): Promise<AccountBalance> {
    val promise: Promise<AccountBalance> = Promise.promise()
    readModelDb.preparedQuery("select * from account_summary where id = $1").execute(Tuple.of(accountId)) { event ->
      if (event.failed()) {
        promise.fail(event.cause())
      } else {
        val set = event.result()
        if (!set.iterator().hasNext()) {
          promise.complete(null)
        } else {
          val row = set.iterator().next()
          val response = AccountBalance(row.getInteger("id"), row.getBigDecimal("balance").toDouble())
          promise.complete(response)
        }
      }
    }
    return promise
  }

  /**
   * Retrieve all accounts from read model
   */
  override fun allAccounts(): Promise<List<AccountBalance>> {
    val promise: Promise<List<AccountBalance>> = Promise.promise()
    readModelDb.preparedQuery("select * from account_summary order by id").execute { event ->
      if (event.failed()) {
        promise.fail(event.cause())
      } else {
        val set = event.result()
        val arrayList = ArrayList<AccountBalance>()
        for (row in set) {
          arrayList.add(AccountBalance(row.getInteger("id"), row.getBigDecimal("balance").toDouble()))
        }
        promise.complete(arrayList)
      }
    }
    return promise
  }
}
