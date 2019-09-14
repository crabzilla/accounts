package com.crabzilla.examples.accounts.service.reports

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool

// TODO mudar para usar 2 bancos e usar 2 repos (read e write)
/**
 * Checks if both read and write models are in sync and consistent
 */
class ConsistencyRepository(private val db: PgPool) {

  /**
   * It retrieve and build a JsonObject with:
   *  1. all accounts within read model that doesn't have it's respective snapshot (write model)
   *  2. all accounts within write model that doesn't have it's respective summary (read model)
   *  3. all accounts with inconsistent balances
   */
  fun inconsistencies(): Promise<JsonObject> {

    val promise = Promise.promise<JsonObject>()
    val result = JsonObject()

    val f1 = missingReadModel()
    val f2 = missingWriteModel()
    val f3 = balanceMismatch()

    CompositeFuture.all(f1, f2, f3)
      .setHandler { event ->
          if (event.succeeded()) {
            if (!f1.result().isEmpty) result.put("missing_read_model", f1.result())
            if (!f2.result().isEmpty) result.put("missing_write_model", f2.result())
            if (!f3.result().isEmpty) result.put("balance_mismatch", f3.result())
            promise.complete(result)
          } else {
            promise.fail(event.cause())
          }
      }

    return promise
  }

  /**
   * Get all accounts within read model that doesn't have it's respective snapshot (write model)
   */
  private fun missingReadModel(): Future<JsonArray> {
    val promise = Promise.promise<JsonArray>()
    val sql = """SELECT account_snapshots.ar_id,
                        (account_snapshots.json_content -> 'balance')::numeric
                   FROM account_snapshots
                  WHERE ar_id not in (SELECT id from account_summary)
                 ORDER by ar_id """.trimMargin()
    db.preparedQuery(sql) { event ->
      if (event.failed()) {
        promise.fail(event.cause()); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("balance", row.getBigDecimal(1).toDouble())
        array.add(jo)
      }
      promise.complete(array)
    }
    return promise.future()
  }

  /**
   * Get all accounts within write model that doesn't have it's respective summary (read model)
   */
  private fun missingWriteModel(): Future<JsonArray> {
    val promise = Promise.promise<JsonArray>()
    val sql = """SELECT account_summary.id, account_summary.balance
                   FROM account_summary
                  WHERE id not in (SELECT ar_id from account_snapshots)
                 ORDER by id """.trimMargin()
    db.preparedQuery(sql) { event ->
      if (event.failed()) {
        promise.fail(event.cause()); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("balance", row.getBigDecimal(1))
        array.add(jo)
      }
      promise.complete(array)
    }
    return promise.future()
  }

  /**
   * get all accounts with inconsistent balances
   */
  private fun balanceMismatch(): Future<JsonArray> {
    val promise = Promise.promise<JsonArray>()
    val sql = """SELECT account_snapshots.ar_id,
                        (account_snapshots.json_content -> 'balance')::numeric as snapshot_balance,
                        account_summary.balance as summary_balance,
                        account_summary.balance - (account_snapshots.json_content -> 'balance')::numeric as difference
                  FROM account_snapshots
                  JOIN account_summary
                    ON account_summary.id = account_snapshots.ar_id
                WHERE account_summary.balance != (account_snapshots.json_content -> 'balance')::numeric
                 ORDER by difference """.trimMargin()
    db.preparedQuery(sql) { event ->
      if (event.failed()) {
        promise.fail(event.cause()); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("snapshot_balance", row.getBigDecimal(1))
                .put("summary_balance", row.getBigDecimal(2)).put("difference", row.getBigDecimal(3))
        array.add(jo)
      }
      promise.complete(array)
    }
    return promise.future()
  }

}