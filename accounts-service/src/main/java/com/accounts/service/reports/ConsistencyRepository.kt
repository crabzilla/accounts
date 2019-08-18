package com.accounts.service

import io.reactiverse.pgclient.PgPool
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

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
  fun inconsistencies(aHandler: Handler<AsyncResult<JsonObject>>) {

    val readModelFuture: Future<JsonArray> = Future.future()
    missingReadModel(readModelFuture)

    val writeModelFuture = Future.future<JsonArray>()
    missingWriteModel(writeModelFuture)

    val mismatchFuture = Future.future<JsonArray>()
    balanceMismatch(mismatchFuture)

    CompositeFuture.all(readModelFuture, writeModelFuture, mismatchFuture).setHandler { event ->
      if (event.succeeded()) {
        val result = JsonObject()
        val readModel = readModelFuture.result()
        val writeModel = writeModelFuture.result()
        val mismatch = mismatchFuture.result()
        if (!readModel.isEmpty) result.put("missing_read_model", readModel)
        if (!writeModel.isEmpty) result.put("missing_write_model", writeModel)
        if (!mismatch.isEmpty) result.put("balance_mismatch", mismatch)
        aHandler.handle(Future.succeededFuture(result))
      } else {
        aHandler.handle(Future.failedFuture(event.cause()))
      }
    }

  }

  /**
   * Get all accounts within read model that doesn't have it's respective snapshot (write model)
   */
  private fun missingReadModel(aHandler: Handler<AsyncResult<JsonArray>>) {
    val sql = """SELECT account_snapshots.ar_id,
                        (account_snapshots.json_content -> 'balance')::numeric
                   FROM account_snapshots
                  WHERE ar_id not in (SELECT id from account_summary)
                 ORDER by ar_id """.trimMargin()
    db.preparedQuery(sql) { event ->
      if (event.failed()) {
        aHandler.handle(Future.failedFuture(event.cause())); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("balance", row.getNumeric(1))
        array.add(jo)
      }
      aHandler.handle(Future.succeededFuture(array))
    }
  }

  /**
   * Get all accounts within write model that doesn't have it's respective summary (read model)
   */
  private fun missingWriteModel(aHandler: Handler<AsyncResult<JsonArray>>) {
    val sql = """SELECT account_summary.id, account_summary.balance
                   FROM account_summary
                  WHERE id not in (SELECT ar_id from account_snapshots)
                 ORDER by id """.trimMargin()
    db.preparedQuery(sql) { event ->
      if (event.failed()) {
        aHandler.handle(Future.failedFuture(event.cause())); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("balance", row.getNumeric(1))
        array.add(jo)
      }
      aHandler.handle(Future.succeededFuture(array))
    }
  }

  /**
   * get all accounts with inconsistent balances
   */
  private fun balanceMismatch(aHandler: Handler<AsyncResult<JsonArray>>) {
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
        aHandler.handle(Future.failedFuture(event.cause())); return@preparedQuery
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0)).put("snapshot_balance", row.getNumeric(1))
                .put("summary_balance", row.getNumeric(2)).put("difference", row.getNumeric(3))
        array.add(jo)
      }
      aHandler.handle(Future.succeededFuture(array))
    }
  }

}