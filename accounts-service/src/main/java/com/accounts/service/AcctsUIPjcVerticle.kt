package com.accounts.service


import io.github.crabzilla.webpgc.DbProjectionsVerticle.Companion.amIAlreadyRunning
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory.getLogger
import java.lang.management.ManagementFactory

class AcctsUIPjcVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(AcctsUIPjcVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
  }

  override fun start(startFuture: Future<Void>) {
    // publishes events to UI
    vertx.eventBus().consumer<String>(config().getString("PROJECTION_ENDPOINT")) { message ->
      val eventsAsJson = JsonObject(message.body())
      vertx.eventBus().publish(config().getString("UI_PROJECTION_ENDPOINT"), eventsAsJson.encodePrettily())
    }
    // a simple mechanism to avoid deploying a HA singleton Verticle on another node
    val projectionEndpoint = this::class.java.name
    vertx.eventBus().consumer<String>(amIAlreadyRunning(projectionEndpoint)) { msg ->
      log.info("received " + msg.body())
      msg.reply("Yes, $projectionEndpoint is already running here: $processId")
    }

    startFuture.complete()
  }

}

