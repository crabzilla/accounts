package com.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Account(
    val accountId: AccountId? = null,
    val balance: Double = 0.00
) :
  Entity() {

  fun deposit(accountId: AccountId, amount: BigDecimal): List<DomainEvent> {
    if (this.accountId == null) {
      return listOf(AccountCreated(accountId), AmountDeposited(amount.toDouble()))
    }
    return listOf(AmountDeposited(amount.toDouble()))
  }

  fun withdrawn(amount: BigDecimal): List<DomainEvent> {
    requireNotNull(this.accountId, { "This account must exists" })
    require(BigDecimal(this.balance).subtract(amount).toInt() >= 0) { "This account does not have enough balance" }
    return listOf(AmountWithdrawn(amount.toDouble()))
  }
}
