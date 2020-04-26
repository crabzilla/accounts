package com.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.internal.CommandContext
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import java.math.BigDecimal

class AccountCmdAware : EntityCommandAware<Account> {

    override val initialState = Account()

    override val applyEvent = { event: DomainEvent, state: Account ->
      when (event) {
        is AccountCreated -> state.copy(accountId = event.accountId)
        is AmountDeposited -> state.copy(balance = state.balance.plus(event.amount))
        is AmountWithdrawn -> state.copy(balance = state.balance.minus(event.amount))
        else -> state
      }
    }

   override val validateCmd: (Command) -> List<String> = { command: Command ->
     when (command) {
      is MakeDeposit ->
        if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      is MakeWithdraw ->
        if (command.amount.toDouble() <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      else -> listOf() // any other commands are valid
    }
  }

  override val handleCmd: (context: CommandContext<Account>) -> Future<List<DomainEvent>> = { context ->
    val (cmdMetadata, command, snapshot) = context
    val account = snapshot.state
    when (command) {
      is MakeDeposit -> succeededFuture(account.deposit(AccountId(cmdMetadata.entityId), BigDecimal(command.amount)))
      is MakeWithdraw -> succeededFuture(account.withdrawn(BigDecimal(command.amount)))
      else -> failedFuture("${cmdMetadata.commandName} is a unknown command")
    }
  }
}
