package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker
import scala.util.Random

object Consignment extends Simulation {

  val faker = new Faker();
  val rnd = new Random()
  val consignmentPrefix = Parameters.stringPrefix

  val consignmentFeeder = Iterator.continually(Map(
    "ref" -> consignmentPrefix.concat("-").concat(rnd.nextInt(1000000).toString),
    "consignmentRef" -> faker.commerce().productName(),
    "width" -> faker.number().numberBetween(10, 100),
    "height" -> faker.number().randomDouble(3, 10, 30),
    "length" -> faker.number().randomDouble(3, 10, 30),
    "weight" -> faker.number().randomDouble(3, 10, 30)
  ))

  val CreateConsignment = {
    feed(consignmentFeeder)
      .exec(http("Create Consignment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateConsignmentInput!) {\n  createConsignment(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","consignmentReference":"${consignmentRef}","carrier":{"id":${carrierId}},"retailer":{"id":${retId}},"consignmentArticles":[{"article":{"id":${articleId}}}]}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createConsignment.id").saveAs("consignmentId"))
        .check(jsonPath("$.data.createConsignment.ref").saveAs("consignmentRef"))
      )
  }

  val CreateCarrierConsignment = {
    feed(consignmentFeeder)
      .exec(http("Create Carrier Consignment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateCarrierConsignmentInput!) {\n  createCarrierConsignment(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","consignmentReference":"${consignmentRef}","carrier":{"id":${carrierId}},"retailer":{"id":${retId}},"carrierConsignmentArticles":[{"article":{"id":${articleId}}}]}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createCarrierConsignment.id").saveAs("carrierConsignmentId"))
        .check(jsonPath("$.data.createCarrierConsignment.ref").saveAs("carrierConsignmentRef"))
      )
  }

  val UpdateConsignment = {
    feed(consignmentFeeder)
      .exec(http("Update Consignment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateConsignmentInput!) {\n  updateConsignment(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${consignmentId},"consignmentReference":"${consignmentRef}"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val UpdateCarrierConsignment = {
    feed(consignmentFeeder)
      .exec(http("Update Carrier Consignment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateCarrierConsignmentInput!) {\n  updateCarrierConsignment(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref": "${carrierConsignmentRef}","consignmentReference":"${consignmentRef}"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetConsignment = {
    feed(Carrier.feeder())
      .exec(http("Get Consignments")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  consignments(first:${feeders.pagination}){\n    edges{\n      node{\n    id\n        ref\n        status\n          status\n        createdOn\n        updatedOn\n                consignmentArticles{\n          edges{\n            node{\n              consignment{\n                ref\n              }\n              article{id\n              ref}\n            }\n          }\n        }\n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.consignments.edges[${feeders.getPagination}].cursor").saveAs("conFirstCursor"))
        .check(jsonPath("$.data.consignments.edges[${feeders.getPagination}].node.id").saveAs("conId"))
      )
  }

  val GetConsignmentByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Consignments By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  consignments(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${conFirstCursor}\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n   id\n        ref\n        status\n              status\n        createdOn\n        updatedOn\n                consignmentArticles{\n          edges{\n            node{\n              consignment{\n                ref\n              }\n              article{id\n              ref}\n            }\n          }\n        }\n      }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetConsignmentById = {
    exec(http("Get Consignments By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query consignmentById(\n$id:ID!\n){\n  consignmentById(id:$id)\n  {\n id\n        ref\n        status\n              status\n        createdOn\n        updatedOn\n                consignmentArticles{\n          edges{\n            node{\n              consignment{\n                ref\n              }\n              article{id\n              ref}\n            }\n          }\n        }\n    }\n}\n",
          |"variables":{"id":"${conId}"},
          |"operationName":"consignmentById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }
  val ConsignmentQueries = scenario("Consignments Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetConsignment, GetConsignmentByPagination, GetConsignmentById)
    }


}



