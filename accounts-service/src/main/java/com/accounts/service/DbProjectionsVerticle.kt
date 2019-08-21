package com.accounts.service


import com.accounts.service.projectors.AccountsSummaryProjector
import io.github.crabzilla.pgc.PgcComponent
import io.github.crabzilla.pgc.PgcProjectionComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import org.slf4j.LoggerFactory.getLogger

class DbProjectionsVerticle : AbstractVerticle() {

  private lateinit var pgcComponent : PgcComponent

  companion object {
    internal val log = getLogger(DbProjectionsVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    pgcComponent = PgcComponent(vertx, config())
    val projectionComponent = PgcProjectionComponent(pgcComponent)
    projectionComponent.addProjector("accounts-summary", AccountsSummaryProjector())
    startFuture.complete()
  }

}

