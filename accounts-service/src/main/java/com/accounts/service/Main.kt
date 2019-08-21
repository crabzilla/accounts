package com.accounts.service

import io.github.crabzilla.initCrabzilla
import io.github.crabzilla.pgc.whoIsRunningProjection
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.*
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

    val hzConfig = ConfigUtil.loadConfig()
    val mgr = HazelcastClusterManager(hzConfig)
    val eventBusOptions = EventBusOptions().setClustered(true)
    val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setEventBusOptions(eventBusOptions)

    Vertx.clusteredVertx(vertxOptions) { gotCluster ->
      if (gotCluster.succeeded()) {
        val vertx = gotCluster.result()
        vertx.initCrabzilla()
        getConfig(vertx).setHandler { gotConfig ->
          if (gotConfig.succeeded()) {
            val config = gotConfig.result()
            val deploymentOptions = DeploymentOptions().setHa(true).setConfig(config)
            val dbProjectionsEndpoint = whoIsRunningProjection(config.getString("PROJECTION_ENDPOINT"))
            CompositeFuture.all(
              deploy(vertx, WebRoutesVerticle::class.java.name, deploymentOptions),
              deploy(vertx, UIProjectionsVerticle::class.java.name, deploymentOptions),
              deploySingletonVerticle(vertx, DbProjectionsVerticle::class.java.name, dbProjectionsEndpoint,
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

  private fun deploy(vertx: Vertx, verticle: String, deploymentOptions: DeploymentOptions): Future<String> {
    val future: Future<String> = Future.future()
    vertx.deployVerticle(verticle, deploymentOptions, future)
    return future
  }

  private fun deploySingletonVerticle(vertx: Vertx, verticle: String, pingEndpoint: String,
                                      dOpt: DeploymentOptions,
                                      processId: Any): Future<String> {
    val future: Future<String> = Future.future()
    vertx.eventBus().send<Any>(pingEndpoint, processId) { isWorking ->
      if (isWorking.succeeded()) {
        log.info("No need to start $verticle: " + isWorking.result().body())
      } else {
        log.info("*** Deploying $verticle")
        vertx.deployVerticle(verticle, dOpt) { wasDeployed ->
          if (wasDeployed.succeeded()) {
            log.info("$verticle started")
            future.complete(wasDeployed.result())
          } else {
            log.error("$verticle not started", wasDeployed.cause())
            future.fail(wasDeployed.cause())
          }
        }
      }
    }
    return future
  }

  private fun getConfig(vertx: Vertx) : Future<JsonObject> {
    val future: Future<JsonObject> = Future.future()
    configRetriever(vertx, CONFIG_PATH).getConfig { gotConfig ->
      if (gotConfig.succeeded()) {
        val config = gotConfig.result()
        log.info("*** config:\n${config.encodePrettily()}")
        val httpPort = config.getInteger("HTTP_PORT")
        val nextFreeHttpPort = nextFreePort(httpPort, httpPort + 20)
        config.put("HTTP_PORT", nextFreeHttpPort)
        log.info("*** next free HTTP_PORT: $nextFreeHttpPort")
        future.complete(config)
      } else {
        future.fail(gotConfig.cause())
      }
    }
    return future
  }

}

