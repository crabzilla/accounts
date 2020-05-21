package accounts

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.util.Random

class BasicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081") // Here is the root for all relative URLs
    .acceptHeader("application/json") // Here are the common headers
    .contentTypeHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Gatling BasicSimulation")

  val feeder = Iterator.continually(Map("accountId" -> Random.nextInt(Int.MaxValue)))

  val scn = scenario("JustOpeningAccounts")
    .feed(feeder) // attaching feeder to session
    .exec(http("OpeningAccounts")
    .post("/commands/account/${accountId}/make-deposit")
    .body(StringBody("""{ "amount": ${accountId}.00 }"""))
  )

  setUp(scn.inject(
    atOnceUsers(100),
    // rampUsers(1000) during (2 minutes),
    constantUsersPerSec(500) during (2 minutes) randomized
  ).protocols(httpProtocol))

}
