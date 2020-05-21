package io.github.crabzilla.examples.accounts.infra.boilerplate

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet

fun cleanDatabase(vertx: Vertx, config: JsonObject): Future<Void> {
  val promise = Promise.promise<Void>()
  val read = readModelPgPool(vertx, config)
  val write = writeModelPgPool(vertx, config)
  write.query("delete from units_of_work").execute { event1: AsyncResult<RowSet<Row?>?> ->
    if (event1.failed()) {
      promise.fail(event1.cause())
      return@execute
    }
    write.query("delete from account_snapshots").execute { event2: AsyncResult<RowSet<Row?>?> ->
      if (event2.failed()) {
        promise.fail(event2.cause())
        return@execute
      }
      read.query("delete from account_summary").execute { event3: AsyncResult<RowSet<Row?>?> ->
        if (event3.failed()) {
          promise.fail(event3.cause())
          return@execute
        }
        promise.complete()
      }
    }
  }
  return promise.future()
}