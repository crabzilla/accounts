package com.accounts.service


import io.github.crabzilla.EventBusChannels
import io.github.crabzilla.webpgc.UowHandlerVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory.getLogger

class AcctsUIProjectionsVerticle : UowHandlerVerticle() {

  companion object {
    internal val log = getLogger(AcctsUIProjectionsVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    super.start()
    // publishes events to UI
    vertx.eventBus().consumer<String>(EventBusChannels.unitOfWorkChannel) { message ->
      val eventsAsJson = JsonObject(message.body())
      vertx.eventBus().publish(config().getString("UI_PROJECTION_ENDPOINT"), eventsAsJson.encodePrettily())
    }
    startFuture.complete()
  }

}

