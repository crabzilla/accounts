package com.crabzilla.examples.accounts.domain

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule

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

val accountsJson = Json(
        configuration = JsonConfiguration(useArrayPolymorphism = false),
        context = accountsModule
)
