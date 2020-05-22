package io.github.crabzilla.examples.accounts

import io.github.crabzilla.examples.accounts.infra.DbProjectionsVerticle
import io.github.crabzilla.examples.accounts.infra.UIProjectionsVerticle
import io.github.crabzilla.examples.accounts.infra.WebCommandVerticle
import io.github.crabzilla.examples.accounts.infra.WebQueryVerticle
import io.github.crabzilla.examples.accounts.infra.boilerplate.deploy
import io.github.crabzilla.examples.accounts.infra.boilerplate.deployHandler
import io.github.crabzilla.examples.accounts.infra.boilerplate.deploySingleton
import io.github.crabzilla.examples.accounts.infra.boilerplate.getConfig
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBusOptions
import io.vertx.spi.cluster.hazelcast.ConfigUtil
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

object
Main {
  private val log: Logger = LoggerFactory.getLogger(Main::class.java)
  private const val CONFIG_PATH = "./accounts.env"

  @JvmStatic
  fun main(args: Array<String>) {
    val cores = Runtime.getRuntime().availableProcessors()
    val processId = ManagementFactory.getRuntimeMXBean().name
    val hzConfig = ConfigUtil.loadConfig()
    val mgr = HazelcastClusterManager(hzConfig)
    val eventBusOptions = EventBusOptions().setClustered(true)
    val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setEventBusOptions(eventBusOptions)
    Vertx.clusteredVertx(vertxOptions) { gotCluster ->
      if (gotCluster.succeeded()) {
        val vertx = gotCluster.result()
        getConfig(vertx, CONFIG_PATH).onComplete { gotConfig ->
          if (gotConfig.succeeded()) {
            val config = gotConfig.result()
            val webOptions = DeploymentOptions().setHa(true).setConfig(config).setInstances(cores)
            val backOptions = DeploymentOptions().setHa(true).setConfig(config).setInstances(1)
            if (args.contains("--backend-only")) {
              CompositeFuture.all(
                deploySingleton(vertx, DbProjectionsVerticle::class.java.name, backOptions, processId),
                deploySingleton(vertx, UIProjectionsVerticle::class.java.name, backOptions, processId))
                .onComplete(deployHandler(vertx))
            } else {
              CompositeFuture.all(
                deploy(vertx, WebCommandVerticle::class.java.name, webOptions),
                deploy(vertx, WebQueryVerticle::class.java.name, webOptions),
                deploySingleton(vertx, DbProjectionsVerticle::class.java.name, backOptions, processId),
                deploySingleton(vertx, UIProjectionsVerticle::class.java.name, backOptions, processId))
                .onComplete(deployHandler(vertx))
            }
          } else {
            log.error("Failed to get config", gotConfig.cause())
          }
        }
      } else {
        log.error("Failed to get HA mode", gotCluster.cause())
      }
    }
  }
}
