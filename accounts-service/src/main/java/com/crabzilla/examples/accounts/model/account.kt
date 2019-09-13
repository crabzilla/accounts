package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.DomainEvent
import io.vertx.core.Promise
import java.math.BigDecimal

// shared model
data class AccountId(val value: Int)

// commands
data class MakeDeposit(val amount: BigDecimal) : Command
data class MakeWithdraw(val amount: BigDecimal) : Command

// events
data class AccountCreated(val accountId: AccountId) : DomainEvent
data class AmountDeposited(val amount: BigDecimal) : DomainEvent
data class AmountWithdrawn(val amount: BigDecimal) : DomainEvent

// read model
data class AccountBalance(val accountId: Int, val balance: BigDecimal)
interface AccountsRepository {
  fun accountById(accountId: Int): Promise<AccountBalance>
  fun allAccounts(): Promise<List<AccountBalance>>
}

