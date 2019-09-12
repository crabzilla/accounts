package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.*
import io.vertx.core.Promise
import io.vertx.core.Promise.failedPromise
import io.vertx.core.Promise.succeededPromise

class AccountCmdHandler(cmdMetadata: CommandMetadata,
                        command: Command,
                        snapshot: Snapshot<AccountEntity>,
                        stateFn: (DomainEvent, AccountEntity) -> AccountEntity) :
        EntityCommandHandler<AccountEntity>("account", cmdMetadata, command, snapshot, stateFn) {

  override fun handleCommand(): Promise<UnitOfWork> {
    val account = snapshot.state
    val eventsPromise = when (command) {
      is MakeDeposit -> succeededPromise(account.deposit(AccountId(cmdMetadata.entityId), command.amount))
      is MakeWithdraw -> succeededPromise(account.withdrawn(command.amount))
      else -> failedPromise("${cmdMetadata.commandName} is a unknown command")
    }
    return fromEvents(eventsPromise)
  }
}