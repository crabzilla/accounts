package com.accounts.model

import io.github.crabzilla.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class AccountCmdAware: EntityCommandAware<AccountEntity> {

  companion object {
    val initialState = AccountEntity()
  }

  override fun initialState(): AccountEntity {
    return initialState
  }

  override fun applyEvent(event: DomainEvent, state: AccountEntity): AccountEntity {
    return when (event) {
      is AccountCreated -> state.copy(accountId = event.accountId)
      is AmountDeposited -> state.copy(balance = state.balance.plus(event.amount))
      is AmountWithdrawn -> state.copy(balance = state.balance.minus(event.amount))
      else -> state
    }
  }

  override fun validateCmd(command: Command): List<String> {
    return when (command) {
      is MakeDeposit -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      is MakeWithdraw -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      else -> listOf() // any other commands are valid
    }
  }

  override fun cmdHandlerFactory(): CommandHandlerFactory<AccountEntity> {
    return { cmdMetadata: CommandMetadata,
             command: Command,
             snapshot: Snapshot<AccountEntity>,
             uowHandler: Handler<AsyncResult<UnitOfWork>> ->
      AccountCmdHandler(cmdMetadata, command, snapshot,
              { event: DomainEvent, account: AccountEntity -> applyEvent(event, account) }, uowHandler)
    }
  }

}