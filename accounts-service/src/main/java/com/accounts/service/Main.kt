package com.accounts.service

import io.github.crabzilla.webpgc.DeploymentConventions.deploy
import io.github.crabzilla.webpgc.DeploymentConventions.deploySingleton
import io.github.crabzilla.webpgc.DeploymentConventions.getConfig
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
            val deploymentOptions = DeploymentOptions().setHa(true).setConfig(config)
            val dbProjectionsEndpoint = config.getString("PROJECTION_ENDPOINT")
            CompositeFuture.all(
              deploy(vertx, AcctsWebVerticle::class.java.name, deploymentOptions),
              deploy(vertx, AcctsUIPjcVerticle::class.java.name, deploymentOptions),
              deploySingleton(vertx, AccountsDbPjcVerticle::class.java.name, dbProjectionsEndpoint,
                      deploymentOptions, processId))
              .setHandler { deploys ->
                if (deploys.succeeded()) {
                  log.info("Verticles were successfully deployed")
                } else {
                  log.error("When deploying", deploys.cause())
                }
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

