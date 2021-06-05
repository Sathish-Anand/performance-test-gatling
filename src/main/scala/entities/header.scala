package entities

import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}

object header extends Simulation {

//  val fluentHeader = Map(
//    "fluent.account" -> "${userAuth.clientId}",
//    "Authorization" -> "bearer ${retAuth}",
//    "Content-Type" -> "application/json"
//   )
//
//  val accountHeader = Map(
//    "fluent.account" -> "${userAuth.clientId}",
//    "Authorization" -> "bearer ${accAuth}",
//    "Content-Type" -> "application/json"
//  )

  val fluentHeader = Map(
    "fluent.account" -> "${clientId}",
    "Authorization" -> "bearer ${retAuth}",
    "Content-Type" -> "application/json"
  )

  val accountHeader = Map(
    "fluent.account" -> "${clientId}",
    "Authorization" -> "bearer ${accAuth}",
    "Content-Type" -> "application/json"
  )

  val superHeader = Map(
    "fluent.account" -> "${userAuth.client_id}",
    "Authorization" -> "bearer ${superAuth}",
    "Content-Type" -> "application/json"
  )

}
