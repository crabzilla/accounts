package com.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.vertx.core.Promise
import kotlinx.serialization.Serializable

// shared model

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

// read model

data class AccountBalance(val accountId: Int, val balance: Double)

interface AccountsRepository {
  fun accountById(accountId: Int): Promise<AccountBalance>
  fun allAccounts(): Promise<List<AccountBalance>>
}

