package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class AccountCmdHandlerFactory : EntityCommandHandlerFactory<AccountEntity> {

  override fun createHandler(cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<AccountEntity>,
                             handler: Handler<AsyncResult<UnitOfWork>>): EntityCommandHandler<AccountEntity> {

    return AccountCmdHandler(cmdMetadata, command, snapshot, AccountCmdAware.acctStateBuilder, handler)
  }

}