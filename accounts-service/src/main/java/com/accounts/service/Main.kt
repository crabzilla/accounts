package com.accounts.service

import io.github.crabzilla.initCrabzilla
import io.github.crabzilla.pgc.whoIsRunningProjection
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBusOptions
import io.vertx.core.json.JsonObject
import io.vertx.spi.cluster.hazelcast.ConfigUtil
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.management.ManagementFactory
import java.net.ServerSocket

object Main {

  private val log: Logger = LoggerFactory.getLogger(Main::class.java)
  private const val CONFIG_PATH = "../accounts.env"

  @JvmStatic
  fun main(args: Array<String>) {

    val processId = ManagementFactory.getRuntimeMXBean().name
    val begin = System.currentTimeMillis()

    val hzConfig = ConfigUtil.loadConfig()
    val mgr = HazelcastClusterManager(hzConfig)
    val eventBusOptions = EventBusOptions().setClustered(true)
    val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setEventBusOptions(eventBusOptions)

    val dOpt = DeploymentOptions().setHa(true)

    Vertx.clusteredVertx(vertxOptions) { gotCluster ->
      run {
        if (gotCluster.succeeded()) {
          val haVertx = gotCluster.result()
          haVertx.initCrabzilla()
          configRetriever(haVertx, CONFIG_PATH).getConfig { gotConfig ->
            if (gotConfig.succeeded()) {
              val config = gotConfig.result()
              log.info("*** config:\n${config.encodePrettily()}")
              dOpt.config = config
              val httpPort = config.getInteger("HTTP_PORT")
              val nextFreeHttpPort = nextFreePort(httpPort, httpPort + 10)
              config.put("HTTP_PORT", nextFreeHttpPort)
              log.info("*** next free HTTP_PORT: $nextFreeHttpPort")
              log.info("*** Deploying app")
              val projectionEndpoint = config.getString("PROJECTION_ENDPOINT")
              val appVerticle = AccountsVerticle::class.java.name
              haVertx.deployVerticle(appVerticle, dOpt) { wasDeployed ->
                if (wasDeployed.succeeded()) {
                  val end1 = System.currentTimeMillis()
                  log.info("$appVerticle started in " + (end1 - begin) + " ms")
                  haVertx.eventBus().send<Any>(whoIsRunningProjection(projectionEndpoint), processId) { isWorking ->
                    if (isWorking.succeeded()) {
                      log.info("No need to start app: " + isWorking.result().body())
                    } else {
                      val projectorVerticle = ProjectorVerticle::class.java.name
                      log.info("*** Deploying $projectorVerticle")
                      haVertx.deployVerticle(projectorVerticle, dOpt) { wasDeployed ->
                        if (wasDeployed.succeeded()) {
                          val end2 = System.currentTimeMillis()
                          log.info("$projectorVerticle started in " + ((end1 + end2) - begin) + " ms")
                        } else {
                          log.error("$projectorVerticle not started", wasDeployed.cause())
                        }
                      }
                    }
                  }
                } else {
                  log.error("$appVerticle not started", wasDeployed.cause())
                }
              }
            } else {
              log.error("Config error", gotConfig.cause())
            }
          }
        } else {
          log.error("Failed to get HA mode", gotCluster.cause())
        }
      }
    }

  }

  private fun configRetriever(vertx: Vertx, configFile: String): ConfigRetriever {
    val envOptions = ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(JsonObject().put("path", configFile))
    val options = ConfigRetrieverOptions().addStore(envOptions)
    return ConfigRetriever.create(vertx, options)
  }

  private fun nextFreePort(from: Int, to: Int): Int {
    var port = from
    while (true) {
      if (isLocalPortFree(port)) {
        return port
      } else {
        if (port == to) {
          throw IllegalStateException("Could not find any from available from $from to $to");
        } else {
          port += 1
        }
      }
    }
  }

  private fun isLocalPortFree(port: Int): Boolean {
    return try {
      log.info("Trying port $port...")
      ServerSocket(port).close()
      true
    } catch (e: IOException) {
      false
    }
  }

}

