package entities

import java.util.concurrent.atomic.AtomicInteger

import com.github.javafaker.Faker
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import util.{Configurations, SetupParameters => Parameters}

import scala.util.Random

object InventoryPositionQuery extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invPos"-> Carrier.get()))
  }

  val GetInventoryPosition = {
    feed(queryFeeder())
      .exec(http("Get Inventory Positions")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  inventoryPositions(first:${invPos.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.inventoryPositions.edges[${invPos.getPagination}].cursor").saveAs("ipFirstCursor"))
        .check(jsonPath("$.data.inventoryPositions.edges[${invPos.getPagination}].node.ref").saveAs("ipRef"))
        .check(jsonPath("$.data.inventoryPositions.edges[${invPos.getPagination}].node.catalogue.ref").saveAs("ipcatalogueRef"))
      )
  }

  val GetInventoryPositionByPagination = {
    feed(queryFeeder())
      .exec(http("Get Inventory Positions By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{"query":"query {\n  inventoryPositions(${invPos.firstOrLast}:${invPos.pagination} ${invPos.afterOrBefore}:\"${ipFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${invPos.from}\",to:\"${invPos.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetInventoryPositionById = {
    exec(http("Get Inventory Positions By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query inventoryPosition(\n$ref:String!,\n  $ipcatalogueRef:String!\n){\n  inventoryPosition(ref:$ref catalogue:{ref:$ipcatalogueRef}){\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           }}\n",
          |"variables":{"ref":"${ipRef}","ipcatalogueRef":"${ipcatalogueRef}"},
          |"operationName":"inventoryPosition"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val InventoryPositionQueries = scenario("Inventory Positions Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetInventoryPosition, GetInventoryPositionByPagination, GetInventoryPositionById)
    }

}
