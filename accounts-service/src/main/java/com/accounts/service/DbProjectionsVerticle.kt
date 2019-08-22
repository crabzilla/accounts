package com.accounts.service


import com.accounts.service.projectors.AccountsSummaryProjector
import io.vertx.core.Future

class DbProjectionsVerticle : PgcDbProjectionsVerticle() {

  override fun start(startFuture: Future<Void>) {
    super.start(startFuture)
    addProjector("accounts-summary", AccountsSummaryProjector())
    startFuture.complete()
  }

}

