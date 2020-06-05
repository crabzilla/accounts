package io.github.crabzilla.examples.accounts.infra

import io.github.crabzilla.core.command.Command
import io.github.crabzilla.examples.accounts.infra.AcctCommand.MakeDeposit
import io.github.crabzilla.examples.accounts.infra.AcctCommand.MakeWithdraw
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
sealed class AcctCommand : Command() {
  @Serializable
  data class MakeDeposit(val amount: Double) : AcctCommand()
  @Serializable
  data class MakeWithdraw(val amount: Double) : AcctCommand()
}

fun main() {
  val mk = MakeDeposit(20.00)
  val mw = MakeWithdraw(22.00)
  val json = Json(JsonConfiguration.Default)
  println(json.stringify(AcctCommand.serializer().list, listOf(mk, mw)))
}
