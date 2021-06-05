package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import scala.util.Random
import com.github.javafaker.Faker


object Workflow extends Simulation {

  val workflowNameGI = List("productCatalogueDefault", "productCatalogueCompatibility", "inventoryCatalogueDefault", "virtualCatalogueBase", "virtualCatalogueNetwork", "location")
//  val workflowNameOrder = List("hdOrder", "ccOrder", "fulfilmentOptions")
  val workflowNameOrder = List("orderHD", "orderCC", "fulfilmentOptions")
  var workflowCount = new AtomicInteger(0)
  var workflowCountOrder = new AtomicInteger(0)
  val rnd = new Random()

  val netCount = new AtomicInteger(1)
  var networkPrefix = Parameters.stringPrefix
  var networkRef = networkPrefix+"_Net_"

  val feeder = Iterator.continually(Map(
    "workflow" -> workflowNameGI(workflowCount.getAndIncrement()),
    "networkName" -> networkRef,
    "networkNumber"-> netCount.getAndIncrement()
  ))

  val orderfeeder = Iterator.continually(Map(
    "delay" -> rnd.nextInt(10),
    "orderWorkflow" -> workflowNameOrder(workflowCountOrder.getAndIncrement())
  ))

  val PutInventoryWorkflow = {
   foreach(_ => workflowNameGI, "Workflow"){
     feed(feeder)
      .exec(http("Update Workflow")
        .put("/api/v4.1/workflow")
        .headers(header.fluentHeader)
        .body(ElFileBody("data/workflow/${workflow}.json"))
        .check(status.is(200))
      )
    }
     .exec(session => {
       workflowCount = new AtomicInteger(0)
       println("Current Value is " + workflowCount)
       session
     }
     )
  }

  val PutOrderWorkflow = {
    foreach(_ => workflowNameOrder, "Workflow"){
      feed(orderfeeder)
        .exec(http("Update Workflow")
          .put("/api/v4.1/workflow")
          .headers(header.fluentHeader)
          .body(ElFileBody("data/workflow/${orderWorkflow}.json"))
          .check(status.is(200))
        )
    }
      .exec(session => {
        workflowCountOrder = new AtomicInteger(0)
        println("Current Value is " + workflowCountOrder)
        session
      }
      )
  }

}
