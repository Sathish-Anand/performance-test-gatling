package entities

import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import authentication.SuperAuthentication
import util.{Configurations, Environment}
import com.github.javafaker.Faker

object Event extends Simulation {

  var faker = new Faker();

  def get(): Map[String, Any] = {
    Map(
      "dbhost" -> "aurora01",
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("event"-> get()))
  }

  val PostEventSetup = {
    feed(feeder())
    .exec(http("Account Event Setup")
      .post("/api/v4.1/event/setup")
      .headers(header.accountHeader)
      .body(StringBody(
        """
          |{
          |		"dbhost":"${event.dbhost}"
          |}
          |""".stripMargin))
      .check(status.is(500))
    )
  }

}
