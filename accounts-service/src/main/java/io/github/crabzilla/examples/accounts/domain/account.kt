package io.github.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

@Serializable
data class AccountId(val value: Int)

// commands
@Serializable
data class MakeDeposit(val amount: Double) : Command()
@Serializable
data class MakeWithdraw(val amount: Double) : Command()

// events
@Serializable
data class AccountCreated(val accountId: AccountId) : DomainEvent()
@Serializable
data class AmountDeposited(val amount: Double) : DomainEvent()
@Serializable
data class AmountWithdrawn(val amount: Double) : DomainEvent()

// command aware
class AccountCmdAware : EntityCommandAware<Account> {
  override val entityName = "account"
  override val initialState = Account()
  override val applyEvent = { event: DomainEvent, state: Account ->
    when (event) {
      is AccountCreated -> state.copy(accountId = event.accountId)
      is AmountDeposited -> state.copy(balance = state.balance + event.amount)
      is AmountWithdrawn -> state.copy(balance = state.balance - event.amount)
      else -> state
    }
  }
  override val validateCmd: (Command) -> List<String> = { command: Command ->
    when (command) {
      is MakeDeposit ->
        if (command.amount <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      is MakeWithdraw ->
        if (command.amount <= 0) listOf("Invalid amount: ${command.amount}") else listOf()
      else -> listOf() // any other commands are valid
    }
  }
  override val handleCmd: (Int, Account, Command) -> Future<List<DomainEvent>> = { id, account, command ->
    when (command) {
      is MakeDeposit -> succeededFuture(account.deposit(AccountId(id), BigDecimal(command.amount)))
      is MakeWithdraw -> succeededFuture(account.withdrawn(BigDecimal(command.amount)))
      else -> failedFuture("${command::class.java} is a unknown command")
    }
  }
}

// aggregate root
@Serializable
data class Account(val accountId: AccountId? = null, val balance: Double = 0.00) : Entity()

fun Account.deposit(accountId: AccountId, amount: BigDecimal): List<DomainEvent> {
  if (this.accountId == null) {
    return listOf(AccountCreated(accountId), AmountDeposited(amount.toDouble()))
  }
  return listOf(AmountDeposited(amount.toDouble()))
}
fun Account.withdrawn(amount: BigDecimal): List<DomainEvent> {
  requireNotNull(this.accountId, { "This account must exists" })
  require(BigDecimal(this.balance).subtract(amount).toInt() >= 0) { "This account does not have enough balance" }
  return listOf(AmountWithdrawn(amount.toDouble()))
}

// json
val accountsModule = SerializersModule {
  polymorphic(Entity::class) {
    Account::class with Account.serializer()
  }
  polymorphic(Command::class) {
    MakeDeposit::class with MakeDeposit.serializer()
    MakeWithdraw::class with MakeWithdraw.serializer()
  }
  polymorphic(DomainEvent::class) {
    AccountCreated::class with AccountCreated.serializer()
    AmountDeposited::class with AmountDeposited.serializer()
    AmountWithdrawn::class with AmountWithdrawn.serializer()
  }
}
