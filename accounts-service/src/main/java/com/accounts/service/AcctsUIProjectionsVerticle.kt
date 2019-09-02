package com.accounts.service


import io.github.crabzilla.EventBusChannels
import io.github.crabzilla.webpgc.UowHandlerVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

class AcctsUIProjectionsVerticle : UowHandlerVerticle() {

  override fun start(startFuture: Future<Void>) {
    super.start()
    // publishes events to UI
    vertx.eventBus().consumer<String>(EventBusChannels.unitOfWorkChannel) { message ->
      val eventsAsJson = JsonObject(message.body())
      vertx.eventBus().publish(config().getString("UI_PROJECTION_CHANNEL"), eventsAsJson.encodePrettily())
    }
    startFuture.complete()
  }

}

