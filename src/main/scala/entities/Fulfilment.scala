package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import scala.util.Random

object Fulfilment extends Simulation {

  val rnd = new Random()
  var fulfilmentPrefix = Parameters.stringPrefix

  val fulfilmentFeeder = Iterator.continually(Map(
    "ref" -> fulfilmentPrefix.concat("-").concat(rnd.nextInt(1000000).toString)
  ))

  val CreateFulfilment = {
    feed(fulfilmentFeeder)
      .exec(http("Create Fulfilment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateFulfilmentInput!) {\n  createFulfilment(input: $input) {\n    id\n    ref\n    type\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","order":{"id":${orderId}},"type":"HD_PFS","deliveryType":"STANDARD"}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createFulfilment.id").saveAs("fulfilmentId"))
      )
  }

  val UpdateFulfilment = {
    feed(fulfilmentFeeder)
      .exec(http("Update Fulfilment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateFulfilmentInput!) {\n  updateFulfilment(input: $input) {\n    id\n    ref\n    type\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${fulfilmentId},"status":"FULFILLED"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetFulfilment = {
    feed(Carrier.feeder())
      .exec(http("Get Fulfilments")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  fulfilments(first:${feeders.pagination}){\n    edges{\n      node{\n   id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        eta\n        expiryTime\n        fromAddress{\n          id\n          ref\n        }\n        fromLocation{\n          ref\n        }\n        createdOn\n        updatedOn\n  }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.fulfilments.edges[${feeders.getPagination}].cursor").saveAs("fulFirstCursor"))
        .check(jsonPath("$.data.fulfilments.edges[${feeders.getPagination}].node.id").saveAs("fulId"))
      )
  }

  val GetFulfilmentByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Fulfilments By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  fulfilments(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${fulFirstCursor}\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n   id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        eta\n        expiryTime\n        fromAddress{\n          id\n          ref\n        }\n        fromLocation{\n          ref\n        }\n        createdOn\n        updatedOn\n    }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetFulfilmentById = {
    exec(http("Get Fulfilments By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query fulfilmentById(\n$id:ID!\n){\n  fulfilmentById(id:$id)\n  { id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        eta\n        expiryTime\n        fromAddress{\n          id\n          ref\n        }\n        fromLocation{\n          ref\n        }\n        createdOn\n        updatedOn\n  }\n}\n",
          |"variables":{"id":"${fulId}"},
          |"operationName":"fulfilmentById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }
  val FulfilmentQueries = scenario("Fulfilments Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetFulfilment, GetFulfilmentByPagination, GetFulfilmentById)
    }

}
