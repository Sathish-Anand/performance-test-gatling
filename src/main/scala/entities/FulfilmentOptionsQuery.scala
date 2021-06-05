package entities

import com.github.javafaker.Faker
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.util.Random

object FulfilmentOptionsQuery extends Simulation {

  val GetFulfilmentOption = {
    feed(Carrier.feeder())
      .exec(http("Get Fulfilment Options")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  fulfilmentOptions(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.fulfilmentOptions.edges[${feeders.getPagination}].cursor").saveAs("foFirstCursor"))
        .check(jsonPath("$.data.fulfilmentOptions.edges[${feeders.getPagination}].node.id").saveAs("foId"))
      )
  }

  val GetFulfilmentOptionByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Fulfilment Options By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  fulfilmentOptions(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${foFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetFulfilmentOptionById = {
    exec(http("Get Fulfilment Options By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query fulfilmentOptionById(\n$id:ID!\n){\n  fulfilmentOptionById(id:$id){\n        id\n        ref\n        type\n             status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"id":"${foId}"},"operationName":"fulfilmentOptionById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val FulfilmentOptionQueries = scenario("Fulfilment Options Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetFulfilmentOption, GetFulfilmentOptionByPagination, GetFulfilmentOptionById)
    }
}
