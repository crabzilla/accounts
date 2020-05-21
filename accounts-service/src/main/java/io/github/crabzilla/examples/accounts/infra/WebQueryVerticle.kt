package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.examples.accounts.infra.boilerplate.listenHandler
import io.github.crabzilla.examples.accounts.infra.boilerplate.readModelPgPool
import io.github.crabzilla.examples.accounts.infra.boilerplate.writeModelPgPool
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory.getLogger

class WebQueryVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(WebQueryVerticle::class.java)
    const val JSON = "application/json"
  }

  private val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  private val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }
  private val httpPort: Int by lazy { config().getInteger("READ_HTTP_PORT") }

  override fun start(promise: Promise<Void>) {

    val config = config()
    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    val repository = DatabaseRepository(writeDb, readDb)

    router.get("/accounts/:id").produces(JSON).handler { rc ->
      val id = rc.pathParam("id").toInt()
      repository.findAccountsSummary(id)
        .onSuccess { result ->
          if (result != null) {
            rc.response().end(JsonObject.mapFrom(result).encodePrettily())
          } else {
            rc.response().setStatusCode(404).end()
          }
        }
        .onFailure { err -> rc.response().setStatusCode(500).end(err.message) }
    }

    router.get("/accounts").produces(JSON).handler { rc ->
      repository.getAllAccountsSummary()
        .onSuccess { result -> rc.response().end(JsonArray(result).encode()) }
        .onFailure { err -> rc.response().setStatusCode(500).end(err.message) }
    }

    router.get("/inconsistencies").handler { rc ->
      insconsistenciesHandler(repository)
        .onSuccess { errors ->
          rc.response().setStatusCode(200).end(errors.encodePrettily())
        }
        .onFailure { err -> rc.response().setStatusCode(500).end(err.message) }
    }

    // eventBus to browser
    val sockJSHandler = SockJSHandler.create(vertx)
    // Allow events for the designated addresses in/out of the event bus bridge
    val options = BridgeOptions().addOutboundPermitted(PermittedOptions()
      .setAddress(config.getString("UI_PROJECTION_CHANNEL")))
    sockJSHandler.bridge(options)
    router.route("/eventbus/*").handler(sockJSHandler)

    // Serve the static resources
    router.route().handler(StaticHandler.create().setIndexPage("index.html").setWebRoot("webroot"))

    // http server
    log.info("READ_HTTP_PORT $httpPort")
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(promise))
  }

  private fun insconsistenciesHandler(repo: DatabaseRepository): Future<JsonArray> {
    val promise = Promise.promise<JsonArray>()
    repo.getAllAccountsSummary()
      .onSuccess { readModel: MutableList<AccountSummary> ->
        repo.getAccountsFromWriteModel()
          .onSuccess { writeModel: MutableList<AccountSummary> ->
            val readMap = readModel.map { it.id to it.balance }.toMap()
            val writeMap = writeModel.map { it.id to it.balance }.toMap()
            val errors = JsonArray()
            if (readMap.size != writeMap.size) {
              val error = "read model has ${readModel.size} accounts while write model has ${writeModel.size} accounts"
              errors.add(JsonObject().put("error", error))
            }
            writeMap.forEach { (id, writeBalance) ->
              val readBalance = readMap[id]
              if (readBalance == null || readBalance.toDouble() != writeBalance.toDouble()) {
                val json = JsonObject().put("id", id)
                  .put("write_balance", writeBalance.toDouble())
                  .put("read_balance", readBalance?.toDouble())
                errors.add(json)
              }
            }
            promise.complete(errors)
          }
          .onFailure { err -> promise.fail(err) }
      }
      .onFailure { err -> promise.fail(err) }
    return promise.future()
  }
}
