package io.github.crabzilla.examples.accounts.infra.boilerplate

import io.vertx.core.Verticle
import io.vertx.kotlin.core.json.jsonObjectOf
import org.slf4j.Logger
import java.lang.management.ManagementFactory

object SingletonVerticleSupport {

  interface SingletonClusteredVerticle: Verticle {

    fun logger(): Logger

    fun addSingletonListener() {
      val verticle = this
      val processId: String = ManagementFactory.getRuntimeMXBean().name
      val verticleClassName = verticle::class.java.name
      // this is only a boilerplate to make sure this verticle is already deployed within a clustered
      vertx.eventBus().consumer<String>(verticleClassName) { areYouThereRequest ->
        if (logger().isDebugEnabled) logger().debug("$verticleClassName received " + areYouThereRequest.body())
        val response = jsonObjectOf(Pair("class", verticleClassName),
          Pair("processId", processId), Pair("deploymentId", vertx.orCreateContext.deploymentID()))
        areYouThereRequest.reply(response)
      }
    }

  }


}
