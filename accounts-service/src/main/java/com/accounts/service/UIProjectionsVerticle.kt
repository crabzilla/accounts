package com.accounts.service


import io.github.crabzilla.UnitOfWorkEvents
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory.getLogger

class UIProjectionsVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(UIProjectionsVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<UnitOfWorkEvents>(config().getString("PROJECTION_ENDPOINT")) { message ->
      val events = message.body()
      val asJson = JsonObject.mapFrom(events)
      vertx.eventBus().publish(config().getString("UI_PROJECTION_ENDPOINT"), asJson.encodePrettily())
    }
    startFuture.complete()
  }

}

