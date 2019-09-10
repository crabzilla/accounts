package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class AccountCmdHandler(cmdMetadata: CommandMetadata,
                        command: Command,
                        snapshot: Snapshot<AccountEntity>,
                        stateFn: (DomainEvent, AccountEntity) -> AccountEntity,
                        uowHandler: Handler<AsyncResult<UnitOfWork>>) :
        EntityCommandHandler<AccountEntity>("account", cmdMetadata, command, snapshot, stateFn, uowHandler) {

  override fun handleCommand() {
    val account = snapshot.state
    when (command) {
      is MakeDeposit -> eventsFuture.complete(account.deposit(AccountId(cmdMetadata.entityId), command.amount))
      is MakeWithdraw -> eventsFuture.complete(account.withdrawn(command.amount))
      else -> eventsFuture.fail("${cmdMetadata.commandName} is a unknown command")
    }
  }
}