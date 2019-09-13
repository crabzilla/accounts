package com.crabzilla.examples.accounts.service.reports

import com.crabzilla.examples.accounts.model.AccountsRepository
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class AccountsWebHandlers(private val accountRepo: AccountsRepository) {

  fun account(rc: RoutingContext) {
    val accountId = try { rc.pathParam("id").toInt() } catch (e: Exception) {
      rc.response().putHeader("Content-Type", "application/json")
              .setStatusCode(400)
              .end("path param entityId must be a number")
      return
    }
    accountRepo.accountById(accountId).future().setHandler(Handler { event ->
      if (event.failed()) {
        rc.response().setStatusCode(500).setStatusMessage(event.cause().message).end(); return@Handler
      }
      val result = event.result()
      if (result == null) {
        rc.response().setStatusCode(404).end("Account not found"); return@Handler
      }
      rc.response().putHeader("Content-Type", "application/json")
              .setStatusCode(200)
              .end(JsonObject.mapFrom(result).encodePrettily())
    })
  }

  fun allAccounts(rc: RoutingContext) {
    accountRepo.allAccounts().future().setHandler(Handler { event ->
       if (event.failed()) {
        rc.response().setStatusCode(500).setStatusMessage(event.cause().message).end();
       } else {
        val result = event.result()
        if (result == null || result.isEmpty()) {
          rc.response().setStatusCode(404).end("Cannot found any Account"); return@Handler
        }
        val json = JsonArray(result).encodePrettily()
        rc.response().putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .setChunked(true)
                .end(json)
      }
    })
  }

}