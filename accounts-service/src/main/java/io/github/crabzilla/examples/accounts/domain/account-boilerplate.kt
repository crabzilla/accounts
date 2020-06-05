package io.github.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.command.AggregateRoot
import io.github.crabzilla.core.command.Command
import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.examples.accounts.domain.Account
import io.github.crabzilla.examples.accounts.domain.AccountCreated
import io.github.crabzilla.examples.accounts.domain.AmountDeposited
import io.github.crabzilla.examples.accounts.domain.AmountWithdrawn
import io.github.crabzilla.examples.accounts.domain.MakeDeposit
import io.github.crabzilla.examples.accounts.domain.MakeWithdraw
import kotlinx.serialization.modules.SerializersModule

// json
val accountsModule = SerializersModule {
  polymorphic(AggregateRoot::class) {
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
