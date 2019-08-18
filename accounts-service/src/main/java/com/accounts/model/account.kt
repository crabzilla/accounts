package com.accounts.model

import io.github.crabzilla.Command
import io.github.crabzilla.DomainEvent
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
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
data class AccountSummary(val accountId: Int, val balance: BigDecimal)
interface AccountsRepository {
  fun accountById(accountId: Int, aHandler: Handler<AsyncResult<AccountSummary>>)
  fun allAccounts(aHandler: Handler<AsyncResult<List<AccountSummary>>>)
}

