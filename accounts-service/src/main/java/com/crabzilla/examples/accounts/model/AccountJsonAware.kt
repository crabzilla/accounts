package com.crabzilla.examples.accounts.model

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.EntityJsonAware
import io.vertx.core.json.JsonObject

class AccountJsonAware : EntityJsonAware<AccountEntity> {

  override fun cmdFromJson(cmdName: String, json: JsonObject): Command {
    return when (cmdName) {
      "make-deposit" -> MakeDeposit(json.getDouble("amount").toBigDecimal())
      "make-withdraw" -> MakeWithdraw(json.getDouble("amount").toBigDecimal())
      else -> throw IllegalArgumentException("$cmdName is unknown")
    }
  }

  override fun cmdToJson(cmd: Command): JsonObject {
    return when (cmd) {
      is MakeDeposit -> JsonObject.mapFrom(cmd)
      is MakeWithdraw -> JsonObject.mapFrom(cmd)
      else -> throw IllegalArgumentException("$cmd is unknown")
    }
  }

  override fun eventFromJson(eventName: String, json: JsonObject): Pair<String, DomainEvent> {
    return when (eventName) {
      "AccountCreated" -> Pair(eventName, AccountCreated(AccountId(json.getJsonObject("accountId").getInteger("value"))))
      "AmountDeposited" -> Pair(eventName, AmountDeposited(json.getDouble("amount").toBigDecimal()))
      "AmountWithdrawn" -> Pair(eventName, AmountWithdrawn(json.getDouble("amount").toBigDecimal()))
      else -> throw IllegalArgumentException("$eventName is unknown")
    }
  }

  override fun eventToJson(event: DomainEvent): JsonObject {
    return when (event) {
      is AccountCreated -> JsonObject.mapFrom(event)
      is AmountDeposited -> JsonObject.mapFrom(event)
      is AmountWithdrawn -> JsonObject.mapFrom(event)
      else -> throw IllegalArgumentException("$event is unknown")
    }
  }

  override fun fromJson(json: JsonObject): AccountEntity {
    return AccountEntity(AccountId(json.getJsonObject("accountId").getInteger("value")),
            json.getDouble("balance").toBigDecimal())
  }

  override fun toJson(entity: AccountEntity): JsonObject {
    return JsonObject.mapFrom(entity)
  }

}