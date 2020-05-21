package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.EventBusChannels.unitOfWorkChannel
import io.github.crabzilla.examples.accounts.infra.boilerplate.addSingletonListener
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UIProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DbProjectionsVerticle::class.java)
  }

  // TODO publish key metrics: accts open, closed activated and deactivations
  // TODO projections listeners should be free (mutiny to compose the model then persist)
  override fun start(promise: Promise<Void>) {

    addSingletonListener(vertx, this::class.java.name)

    // publishes events to UI
    vertx.eventBus().consumer<JsonObject>(unitOfWorkChannel) { message ->
      val eventsAsJson = message.body()
      vertx.eventBus().publish(config().getString("UI_PROJECTION_CHANNEL"), eventsAsJson.encodePrettily())
    }

    promise.complete()
  }

}
