package io.github.crabzilla.examples.accounts

import io.github.crabzilla.examples.accounts.infra.DatabaseProjectionsVerticle
import io.github.crabzilla.examples.accounts.infra.EventbusProjectionsVerticle
import io.github.crabzilla.examples.accounts.infra.UIProjectionsVerticle
import io.github.crabzilla.examples.accounts.infra.WebCommandVerticle
import io.github.crabzilla.examples.accounts.infra.WebQueryVerticle
import io.github.crabzilla.examples.accounts.infra.boilerplate.ConfigSupport.getConfig
import io.github.crabzilla.examples.accounts.infra.boilerplate.DeploySupport.deploy
import io.github.crabzilla.examples.accounts.infra.boilerplate.DeploySupport.deployHandler
import io.github.crabzilla.examples.accounts.infra.boilerplate.DeploySupport.deploySingleton
import io.github.crabzilla.examples.accounts.infra.boilerplate.HttpSupport
import io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.SingletonClusteredVerticle
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
    val projectionVerticle = if (args.contains("--db"))
        DatabaseProjectionsVerticle()::class.java.name else EventbusProjectionsVerticle()::class.java.name
    log.info("Using ${projectionVerticle::class.java.simpleName}")
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
            // find free http ports
            val candidatePorts = generateSequence(8080) { it + 1 }.take(20).toList()
            val writePort = HttpSupport.findFreePort(candidatePorts, listOf())
            config.put("WRITE_HTTP_PORT", writePort)
            config.put("READ_HTTP_PORT", HttpSupport.findFreePort(candidatePorts, listOf(writePort)))
            val webOptions = DeploymentOptions().setHa(true).setConfig(config).setInstances(cores)
            val backOptions = DeploymentOptions().setHa(true).setConfig(config).setInstances(1)
            if (args.contains("--backend-only")) {
              CompositeFuture.all(
                deploySingleton(vertx, projectionVerticle, backOptions, processId),
                deploySingleton(vertx, UIProjectionsVerticle::class.java.name, backOptions, processId))
                .onComplete(deployHandler(vertx))
            } else {
              CompositeFuture.all(
                deploy(vertx, WebCommandVerticle::class.java.name, webOptions),
                deploy(vertx, WebQueryVerticle::class.java.name, webOptions),
                deploySingleton(vertx, projectionVerticle, backOptions, processId),
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
