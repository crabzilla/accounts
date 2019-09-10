package com.crabzilla.examples.accounts.model

import com.crabzilla.examples.accounts.model.AccountCmdAware.Companion.acctStateBuilder
import io.github.crabzilla.framework.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class AccountCmdAware: EntityCommandAware<AccountEntity> {

  companion object {
    val initialState = AccountEntity()
    val acctStateBuilder = { event: DomainEvent, state: AccountEntity ->
      when (event) {
        is AccountCreated -> state.copy(accountId = event.accountId)
        is AmountDeposited -> state.copy(balance = state.balance.plus(event.amount))
        is AmountWithdrawn -> state.copy(balance = state.balance.minus(event.amount))
        else -> state
      }
    }
  }

  override fun initialState(): AccountEntity {
    return initialState
  }

  override fun applyEvent(event: DomainEvent, state: AccountEntity): AccountEntity {
    return acctStateBuilder.invoke(event, state)
  }

  override fun validateCmd(command: Command): List<String> {
    return when (command) {
      is MakeDeposit -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      is MakeWithdraw -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      else -> listOf() // any other commands are valid
    }
  }

  override fun cmdHandlerFactory(): EntityCommandHandlerFactory<AccountEntity> {
    return AccountCmdHandlerFactory()
  }

}

class AccountCmdHandlerFactory : EntityCommandHandlerFactory<AccountEntity> {
  override fun createHandler(cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<AccountEntity>,
                             handler: Handler<AsyncResult<UnitOfWork>>): EntityCommandHandler<AccountEntity> {
    return AccountCmdHandler(cmdMetadata, command, snapshot, acctStateBuilder, handler)
  }
}
