package com.accounts.model

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.Entity
import java.math.BigDecimal

data class AccountEntity(val accountId: AccountId? = null, val balance: BigDecimal = BigDecimal(0)) : Entity {

  fun deposit(accountId: AccountId, amount: BigDecimal): List<DomainEvent> {
    if (this.accountId == null) {
      return eventsOf(AccountCreated(accountId), AmountDeposited(amount))
    }
    return eventsOf(AmountDeposited(amount))
  }

  fun withdrawn(amount: BigDecimal): List<DomainEvent> {
    requireNotNull(this.accountId, {"This account must exists"} )
    require(this.balance.subtract(amount).toInt() >= 0) {"This account does not have enough balance"}
    return eventsOf(AmountWithdrawn(amount))
  }

}