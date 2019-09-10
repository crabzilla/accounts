package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.EntityCommandAware
import io.github.crabzilla.framework.EntityCommandHandlerFactory

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

