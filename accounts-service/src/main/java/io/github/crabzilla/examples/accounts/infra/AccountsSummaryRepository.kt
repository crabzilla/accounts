package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.examples.accounts.infra.datamodel.tables.daos.AccountSummaryDao
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DefaultConfiguration

class AccountsSummaryRepository(private val writeDb: PgPool, readDb: PgPool) {

  private val jooq = DefaultConfiguration()
  private val dao = AccountSummaryDao(jooq, readDb)
  init {
    jooq.setSQLDialect(POSTGRES)
  }

  fun find(id: Int): Future<AccountSummary?> {
    return dao.findOneById(id)
  }

  fun getAll(): Future<MutableList<AccountSummary>> {
    return dao.findAll()
  }

  fun getFromWriteModel(): Future<MutableList<AccountSummary>> {
    val promise = Promise.promise<MutableList<AccountSummary>>()
    val sql = """SELECT ar_id as id, (json_content -> 'balance')::numeric as balance
                        FROM crabz_account_snapshots ORDER by ar_id """
    writeDb.preparedQuery(sql).execute { event ->
      if (event.failed()) {
        event.cause().printStackTrace()
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
