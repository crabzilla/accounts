package io.github.crabzilla.examples.accounts.infra.boilerplate

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Promise.promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.SLF4JLogDelegateFactory
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.net.ServerSocket

private val log = LoggerFactory.getLogger("web-pgc-infra")

fun findFreeHttpPort(): Int {
  var httpPort = 0
  try {
    val socket = ServerSocket(0)
    httpPort = socket.localPort
    socket.close()
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return httpPort
}

fun getConfig(vertx: Vertx, configFile: String): Future<JsonObject> {
  // slf4j setup
  System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
    SLF4JLogDelegateFactory::class.java.name)
  LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory::class.java)
  // Jackson setup
  DatabindCodec.mapper()
    .registerModule(Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  // get config
  val promise = promise<JsonObject>()
  configRetriever(vertx, configFile).getConfig { gotConfig ->
    if (gotConfig.succeeded()) {
      val config = gotConfig.result()
      log.info("*** config:\n${config.encodePrettily()}")
      val httpPort = config.getInteger("HTTP_PORT")
      log.info("*** HTTP_PORT: $httpPort")
      promise.complete(config)
    } else {
      promise.fail(gotConfig.cause())
    }
  }
  return promise.future()
}

private fun configRetriever(vertx: Vertx, configFile: String): ConfigRetriever {
  val envOptions = ConfigStoreOptions()
    .setType("file")
    .setFormat("properties")
    .setConfig(JsonObject().put("path", configFile))
  val options = ConfigRetrieverOptions().addStore(envOptions)
  return ConfigRetriever.create(vertx, options)
}

fun deploy(vertx: Vertx, verticle: String, deploymentOptions: DeploymentOptions): Future<String> {
  val promise = promise<String>()
  vertx.deployVerticle(verticle, deploymentOptions, promise)
  return promise.future()
}

fun deploySingleton(vertx: Vertx, verticle: String, dOpt: DeploymentOptions, processId: String): Future<String> {
  val promise = promise<String>()
  vertx.eventBus().request<String>(verticle, processId) { isWorking ->
    if (isWorking.succeeded()) {
      log.info("No need to start $verticle: " + isWorking.result().body())
    } else {
      log.info("*** Deploying $verticle")
      vertx.deployVerticle(verticle, dOpt) { wasDeployed ->
        if (wasDeployed.succeeded()) {
          log.info("$verticle started")
          promise.complete("singleton ${wasDeployed.result()}")
        } else {
          log.error("$verticle not started", wasDeployed.cause())
          promise.fail(wasDeployed.cause())
        }
      }
    }
  }
  return promise.future()
}

fun deployHandler(vertx: Vertx): Handler<AsyncResult<CompositeFuture>> {
  return Handler { deploys ->
    if (deploys.succeeded()) {
      val deploymentIds = deploys.result().list<String>()
      log.info("Verticles were successfully deployed")
      Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
          for (id in deploymentIds) {
            if (id.startsWith("singleton")) {
              log.info("Keeping singleton deployment $id")
            } else {
              log.info("Undeploying $id")
              vertx.undeploy(id)
            }
          }
          log.info("Closing vertx")
          vertx.close()
        }
      })
    } else {
      log.error("When deploying", deploys.cause())
    }
  }
}

fun listenHandler(promise: Promise<Void>): Handler<AsyncResult<HttpServer>> {
  return Handler { startedFuture ->
    if (startedFuture.succeeded()) {
      log.info("Server started on port " + startedFuture.result().actualPort())
      promise.complete()
    } else {
      log.error("oops, something went wrong during server initialization", startedFuture.cause())
      promise.fail(startedFuture.cause())
    }
  }
}

fun addSingletonListener(vertx: Vertx, implClazz: String) {
  val processId: String = ManagementFactory.getRuntimeMXBean().name
  // this is only a boilerplate to make sure this verticle runs as a clustered singleton
  vertx.eventBus().consumer<String>(implClazz) { msg ->
    if (log.isDebugEnabled) log.debug("$implClazz received " + msg.body())
    msg.reply("$implClazz is already running here: $processId")
  }
}

