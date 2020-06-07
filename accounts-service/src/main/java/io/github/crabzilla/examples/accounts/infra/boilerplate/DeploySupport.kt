package io.github.crabzilla.examples.accounts.infra.boilerplate

import io.github.crabzilla.examples.accounts.infra.boilerplate.SingletonVerticleSupport.SingletonClusteredVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DeploySupport {

  private val log: Logger = LoggerFactory.getLogger(DeploySupport::class.java)

  @JvmStatic
  fun deploy(vertx: Vertx, verticle: String, deploymentOptions: DeploymentOptions): Future<String> {
    val promise = Promise.promise<String>()
    vertx.deployVerticle(verticle, deploymentOptions, promise)
    return promise.future()
  }

  @JvmStatic
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

  @JvmStatic
  fun deploySingleton(vertx: Vertx, verticleClassName: String, dOpt: DeploymentOptions, processId: String):
    Future<String> {
    val promise = Promise.promise<String>()
    vertx.eventBus().request<JsonObject>(verticleClassName, processId) { gotResponse ->
      if (gotResponse.succeeded()) {
        log.info("No need to deploy $verticleClassName: " + gotResponse.result().body().encodePrettily())
      } else {
        log.info("*** Deploying $verticleClassName")
        vertx.deployVerticle(verticleClassName, dOpt) { wasDeployed ->
          if (wasDeployed.succeeded()) {
            log.info("$verticleClassName started")
            promise.complete("singleton ${wasDeployed.result()}")
          } else {
            log.error("$verticleClassName not started", wasDeployed.cause())
            promise.fail(wasDeployed.cause())
          }
        }
      }
    }
    return promise.future()
  }
}
