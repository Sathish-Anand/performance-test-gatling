package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import dataSetup._
import scala.concurrent.duration._
import com.github.javafaker.Faker
import scala.util.Random
import io.gatling.http.Predef._
import java.time.OffsetDateTime
import java.time.Duration
import java.time.Clock


object Carrier extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  val currentTime = OffsetDateTime.now(Clock.systemUTC)
  val firstAndLast = Array("first", "last")
  val beforeAndAfter = Array("before", "after")

  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "id" ->  rnd.nextInt(1000),
      "pagination" ->  (rnd.nextInt(100)+10),
      "getPagination" -> rnd.nextInt(5),
      "from" -> "2020-10-23T08:01:35.858Z",
      "to" -> OffsetDateTime.now(Clock.systemUTC),
      "firstOrLast" -> rnd.shuffle(firstAndLast.toList).head,
      "afterOrBefore" -> rnd.shuffle(beforeAndAfter.toList).head
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("feeders"-> get()))
  }

  val noOfRepeat = 2

  val PostCarrier = {
    exec(http("Create Carrier")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(ElFileBody("data/dataSetup/carrier.json"))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("carRef"))
    )
  }

  val GetCarrier = {
      feed(feeder())
    .exec(http("Get Carriers")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"{\n  carriers(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.carriers.edges[${feeders.getPagination}].cursor").saveAs("carFirstCursor"))
      .check(jsonPath("$.data.carriers.edges[${feeders.getPagination}].node.ref").saveAs("carRef"))
    )
  }

  val GetCarrierByPagination = {
    feed(feeder())
    .exec(http("Get Carrier By Pagination")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query carrier($first:Int,\n  $after:String,\n  $from:DateTime,\n  $to:DateTime \n){\n  carriers(first:$first after:$after status:\"CREATED\" createdOn:{from:$from,to:$to} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}",
          |"variables":{"${feeders.firstOrLast}":${feeders.pagination}, "${feeders.afterOrBefore}":"${carFirstCursor}","from":"${feeders.from}","to":"${feeders.to}"},
          |"operationName":"carrier"}
          |""".stripMargin))
      .check(status.is(200))
    )
  }

  val GetCarrierById = {
    exec(http("Get Carrier By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query carrier(\n$ref:String!\n){\n  carrier(ref:$ref){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${carRef}"},"operationName":"carrier"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val CarrierQueries = scenario("Carriers Query search, byId, with paginations")
    .repeat(noOfRepeat) {
      exec(GetCarrier, GetCarrierByPagination, GetCarrierById)
    }
}
