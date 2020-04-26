package com.crabzilla.examples.accounts.infra

import io.github.crabzilla.pgc.PgcEventBusChannels.unitOfWorkChannel
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject

class AcctsUIProjectionsVerticle : AbstractVerticle() {

  override fun start(promise: Promise<Void>) {
    super.start()
    // publishes events to UI
    vertx.eventBus().consumer<String>(unitOfWorkChannel) { message ->
      val eventsAsJson = JsonObject(message.body())
      vertx.eventBus().publish(config().getString("UI_PROJECTION_CHANNEL"), eventsAsJson.encodePrettily())
    }
    promise.complete()
  }
}
