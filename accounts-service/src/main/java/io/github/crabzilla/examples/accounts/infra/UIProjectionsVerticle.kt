package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.command.EventBusChannels
import io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.SingletonClusteredVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// publishes events to UI
class UIProjectionsVerticle : AbstractVerticle(), SingletonClusteredVerticle {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DatabaseProjectionsVerticle::class.java)
  }

  override fun logger(): Logger {
    return log
  }

  // TODO publish key metrics: accts open, closed activated and deactivations
  // TODO projections listeners should be free (mutiny to compose the model then persist)
  override fun start(promise: Promise<Void>) {

    addSingletonListener()

    val channel = EventBusChannels.aggregateRootChannel("account")
    vertx.eventBus().consumer<JsonObject>(channel) { message ->
      val eventsAsJson = message.body()
      vertx.eventBus().publish(config().getString("UI_PROJECTION_CHANNEL"), eventsAsJson.encodePrettily())
    }

    promise.complete()
  }
}
