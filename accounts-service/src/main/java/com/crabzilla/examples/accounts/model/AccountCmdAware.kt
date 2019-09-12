package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.*

class AccountCmdAware: EntityCommandAware<AccountEntity> {

    override val initialState = AccountEntity()

    override val applyEvent = { event: DomainEvent, state: AccountEntity ->
      when (event) {
        is AccountCreated -> state.copy(accountId = event.accountId)
        is AmountDeposited -> state.copy(balance = state.balance.plus(event.amount))
        is AmountWithdrawn -> state.copy(balance = state.balance.minus(event.amount))
        else -> state
      }
    }

   override val validateCmd: (Command) -> List<String> = { command: Command ->
     when (command) {
      is MakeDeposit -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      is MakeWithdraw -> if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      else -> listOf() // any other commands are valid
    }
  }

  override val cmdHandlerFactory: (cmdMetadata: CommandMetadata,
                                   command: Command,
                                   snapshot: Snapshot<AccountEntity>) -> EntityCommandHandler<AccountEntity> = {
    cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<AccountEntity> ->
    AccountCmdHandler(cmdMetadata, command, snapshot, applyEvent)
  }

}

