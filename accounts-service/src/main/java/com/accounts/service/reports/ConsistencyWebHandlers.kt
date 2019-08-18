package com.accounts.service

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

class ConsistencyWebHandlers(private val consistencyRepo: ConsistencyRepository) {

  fun inconsistencies(rc: RoutingContext) {
    consistencyRepo.inconsistencies(Handler { event ->
      if (event.failed()) {
        rc.response().setStatusCode(500).setStatusMessage(event.cause().message).end(); return@Handler
      }
      val result = event.result()
      if (result == null || result.isEmpty) {
        rc.response().setStatusCode(404).end("Both write and read models seems to be consistent, yay!");
        return@Handler
      }
      rc.response().putHeader("Content-Type", "application/json").setStatusCode(200).setChunked(true)
              .end(result.encode())
    })
  }

}