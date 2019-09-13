package com.crabzilla.examples.accounts.service

import io.github.crabzilla.webpgc.deploy
import io.github.crabzilla.webpgc.deployHandler
import io.github.crabzilla.webpgc.deploySingleton
import io.github.crabzilla.webpgc.getConfig
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBusOptions
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.spi.cluster.hazelcast.ConfigUtil
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory


object Main {
  private val log: Logger = LoggerFactory.getLogger(Main::class.java)
  private const val CONFIG_PATH = "../accounts.env"
  @JvmStatic
  fun main(args: Array<String>) {
    val processId = ManagementFactory.getRuntimeMXBean().name
    val hzConfig = ConfigUtil.loadConfig()
    val mgr = HazelcastClusterManager(hzConfig)
    val eventBusOptions = EventBusOptions().setClustered(true)
    val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setEventBusOptions(eventBusOptions)
    Vertx.clusteredVertx(vertxOptions) { gotCluster ->
      if (gotCluster.succeeded()) {
        val vertx = gotCluster.result()
        getConfig(vertx, CONFIG_PATH).setHandler { gotConfig ->
          if (gotConfig.succeeded()) {
            val config = gotConfig.result()
            val webOptions = DeploymentOptions().setHa(true).setConfig(config)
            val backOptions = DeploymentOptions().setHa(true).setConfig(config).setInstances(1)
            CompositeFuture.all(
              deploy(vertx, AcctsWebCommandVerticle::class.java.name, webOptions),
              deploy(vertx, AcctsWebQueryVerticle::class.java.name, webOptions),
              deploySingleton(vertx, AcctsDbProjectionsVerticle::class.java.name, backOptions, processId),
              deploySingleton(vertx, AcctsUIProjectionsVerticle::class.java.name, backOptions, processId))
            .setHandler(deployHandler(vertx))
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

