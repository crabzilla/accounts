package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.CrabzillaContext
import io.github.crabzilla.examples.accounts.domain.AccountCmdAware
import io.github.crabzilla.examples.accounts.domain.MakeDeposit
import io.github.crabzilla.examples.accounts.domain.MakeWithdraw
import io.github.crabzilla.examples.accounts.domain.accountsModule
import io.github.crabzilla.examples.accounts.infra.boilerplate.listenHandler
import io.github.crabzilla.examples.accounts.infra.boilerplate.writeModelPgPool
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.PgcUowJournal
import io.github.crabzilla.pgc.PgcUowRepo
import io.github.crabzilla.web.WebResourceContext
import io.github.crabzilla.web.addResourceForEntity
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory.getLogger

class WebCommandVerticle : AbstractVerticle() {

  companion object {
    internal val log = getLogger(WebCommandVerticle::class.java)
  }

  private val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }
  private val httpPort: Int by lazy { config().getInteger("WRITE_HTTP_PORT") }

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // map commands routes to it's class
    val cmdTypeMap = mapOf(
            Pair("make-deposit", MakeDeposit::class.qualifiedName as String),
            Pair("make-withdraw", MakeWithdraw::class.qualifiedName as String))

    val accountsJson = Json(context = accountsModule)
    val uowJournal = PgcUowJournal(vertx, writeDb, accountsJson)
    val ctx = CrabzillaContext(accountsJson, PgcUowRepo(writeDb, accountsJson), uowJournal)
    val cmdAware = AccountCmdAware()
    val snapshotRepo = PgcSnapshotRepo(writeDb, accountsJson, cmdAware) // TO write to db
    // val snapshotRepo = InMemorySnapshotRepository(vertx.sharedData(), accountsJson, cmdAware)

    val resourceContext = WebResourceContext(cmdTypeMap, cmdAware, snapshotRepo)
    addResourceForEntity(router, ctx, resourceContext)

    // http server
    log.info("WRITE_HTTP_PORT $httpPort")
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(promise))
  }
}
