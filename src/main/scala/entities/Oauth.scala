package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.util.Properties
import com.github.javafaker.Faker

object Oauth extends Simulation {

  def get(): Map[String, Any] = {
    Map(
      "username" -> "${accUsername}",
      "password" -> "${accPassword}",
      "retUsername" -> "${retUsername}",
      "retPassword" -> "${retpassword}",
      "clientId" -> "${clientId}",
      "grant_type" -> "password",
      "scope" -> "api",
      "client_secret" -> "${clientSecret}"
    )
  }

  var account = Parameters.accountName
  var usernameCount = new AtomicInteger(1)
  var passwordCount = new AtomicInteger(1)
  val noOfRetailer = Parameters.retailer

  var usernm = 1
  var password = 1

  val authFeeder = jsonFile("data/accounts/" + account + ".json").circular


  def authget(): Map[String, Any] = {
    Map(
      "user" -> usernameCount.getAndIncrement(),
      "pass" -> passwordCount.getAndIncrement()
    )
  }

  //
  def authenticationfeeder(): Iterator[Map[String, Any]] = {
    Iterator.continually(Map("authenticate" -> authget()))
  }

  def feeder(): Iterator[Map[String, Any]] = {
    Iterator.continually(Map("auth" -> get()))
  }

  val AccountAuth = {
    feed(feeder())
      .exec(http("Account Authentication")
        .post("/oauth/token")
        .queryParam("username", "${accUsername}")
        .queryParam("password", "${accPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "${auth.scope}")
        .queryParam("grant_type", "${auth.grant_type}")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("accAuth"))
        .check(jsonPath("$.FirstName").saveAs("accName"))
      )
  }

  val RetailerAuth = {
    feed(feeder())
      .exec(http("Retailer Authentication")
        .post("/oauth/token")
        .queryParam("username", "${retUsername}")
        .queryParam("password", "${retPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "${auth.scope}")
        .queryParam("grant_type", "${auth.grant_type}")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("retAuth"))
        .check(jsonPath("$.Retailer_id").saveAs("retId"))
        .check(jsonPath("$.FirstName").saveAs("retName"))
      )
  }

  val JsonAccountAuth = {
    feed(authFeeder)
      .exec(http("Account Authentication")
        .post("/oauth/token")
        .queryParam("username", "${accUsername}")
        .queryParam("password", "${accPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "api")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("accAuth"))
        .check(jsonPath("$.FirstName").saveAs("accName"))
      )
  }

  val JsonRetailerAuth = {
    feed(authFeeder)
      //      .feed(authenticationfeeder())
      .exec(session => {
        var counter = 1
//          (session("counter").as[Int] + 1).toString
        var username = session("retUsername_" + counter).as[String]
        var password = session("retPassword_" + counter).as[String]
        session.set("retUsername", username).set("retPassword", password)
      })
      .exec(http("Retailer Authentication")
        .post("/oauth/token")
        .queryParam("username", "${retUsername}")
        .queryParam("password", "${retPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "api")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("retAuth"))
        .check(jsonPath("$.Retailer_id").saveAs("retId"))
        .check(jsonPath("$.FirstName").saveAs("retName"))
      )
  }

  val JsonRetailerAuth_1 = {
    feed(authFeeder)
      //      .feed(authenticationfeeder())
      .exec(session => {
        var counters = (session("counters").as[Int] + 1).toString
        var username = session("retUsername_" + counters).as[String]
        var password = session("retPassword_" + counters).as[String]
        session.set("retUsername", username).set("retPassword", password)
      })
      .exec(http("Retailer Authentication")
        .post("/oauth/token")
        .queryParam("username", "${retUsername}")
        .queryParam("password", "${retPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "api")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("retAuth"))
        .check(jsonPath("$.Retailer_id").saveAs("retId"))
        .check(jsonPath("$.FirstName").saveAs("retName"))
      )
  }

  val JsonRetailerAuth_2 = {
    feed(authFeeder)
      //      .feed(authenticationfeeder())
      .exec(session => {
        var counter2 = (session("counter2").as[Int] + 1).toString
        var username = session("retUsername_" + counter2).as[String]
        var password = session("retPassword_" + counter2).as[String]
        session.set("retUsername", username).set("retPassword", password)
      })
      .exec(http("Retailer Authentication")
        .post("/oauth/token")
        .queryParam("username", "${retUsername}")
        .queryParam("password", "${retPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "api")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("retAuth"))
        .check(jsonPath("$.Retailer_id").saveAs("retId"))
        .check(jsonPath("$.FirstName").saveAs("retName"))
      )
  }
}
