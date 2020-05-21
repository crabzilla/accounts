package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.examples.accounts.infra.datamodel.tables.daos.AccountSummaryDao
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DefaultConfiguration

class DatabaseRepository(private val writeDb: PgPool, private val readDb: PgPool) {

  private val jooq = DefaultConfiguration()
  private val dao = AccountSummaryDao(jooq, readDb)
  init {
    jooq.setSQLDialect(POSTGRES)
  }

  fun findAccountsSummary(id: Int): Future<AccountSummary> {
    return dao.findOneById(id)
  }

  fun getAllAccountsSummary(): Future<MutableList<AccountSummary>> {
    return dao.findAll()
  }

  fun getAccountsFromWriteModel(): Future<MutableList<AccountSummary>> {
    val promise = Promise.promise<MutableList<AccountSummary>>()
    val sql = """SELECT account_snapshots.ar_id as id,
                              (account_snapshots.json_content -> 'balance')::numeric as balance
                        FROM account_snapshots
                       ORDER by account_snapshots.ar_id """
    writeDb.preparedQuery(sql).execute { event ->
      if (event.failed()) {
        promise.fail(event.cause())
        return@execute
      }
      val writeModel = mutableListOf<AccountSummary>()
      val set = event.result()
      for (row in set) {
        writeModel.add(AccountSummary(row.getInteger(0), row.getBigDecimal(1)))
      }
      promise.complete(writeModel)
    }
    return promise.future()
  }
}
