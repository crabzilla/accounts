package com.accounts.service


import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory.getLogger

class AcctsUIPjcVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(AcctsUIPjcVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<String>(config().getString("PROJECTION_ENDPOINT")) { message ->
      val eventsAsJson = JsonObject(message.body())
      vertx.eventBus().publish(config().getString("UI_PROJECTION_ENDPOINT"), eventsAsJson.encodePrettily())
    }
    startFuture.complete()
  }

}

