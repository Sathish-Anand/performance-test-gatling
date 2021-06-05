package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario._
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.util.concurrent.atomic.AtomicInteger

import com.github.javafaker.Faker

import scala.util.Random

object InventoryQuantity extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  var users = (Configurations.users*2)
  var iqCount = new AtomicInteger(1)
  var ipCount = new AtomicInteger(1)
  var InventoryQuantityPositionCount = new AtomicInteger(1)

  var InventoryQuantityPrefix = Parameters.stringPrefix
  var InventoryQuantityRef = InventoryQuantityPrefix + "_IQ_"
  var InventoryPositionRef = InventoryQuantityPrefix + "_IP_"


  var totalStandardProduct = Parameters.standardProduct
  var totalVariantOnlyProduct = Parameters.variantOnlyProduct
  var totalStandardOnlyProduct = Parameters.standardOnlyProduct
  var totalVariantProduct = Parameters.standardVariantProduct
  var totalLocations = Parameters.network * Parameters.location

  var inventoryPositions = (((totalStandardProduct * totalVariantProduct) + totalVariantOnlyProduct + totalStandardOnlyProduct) * totalLocations)/users

  var totalInventoryPositions = Math.round(inventoryPositions)

  def get(): Map[String, Any] = {
    Map(
      "condition" -> "NEW",
      "name" -> faker.commerce().productName(),
      "iqRef" -> InventoryQuantityRef,
      "number" -> iqCount.getAndIncrement(),
      "ipRef" -> InventoryPositionRef,
      "ipNumber" -> ipCount.getAndIncrement(),
      "type" -> "DEFAULT",
      "quantity" -> rnd.nextInt(100000)
    )}


  def inventoryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invQty"-> get()))
  }

  val PostInventoryQuantity = {
    feed(inventoryFeeder())
    .exec(http("Create Inventory Quantity For Each Inventory Positions")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """|{
           |"query":"mutation createInventoryQuantity($ref:String!, \n  $type:String! $catRef:String! $positionRef:String! $condition:String! $quantity:Int!){\n    createInventoryQuantity(input:{\n   ref:$ref\n    catalogue:{\n      ref:$catRef\n    }\n    position:{\n      ref:$positionRef\n      catalogue:{\n        ref:$catRef\n      }\n    }\n    type:$type\n    quantity:$quantity\n    condition:$condition\n  }){\n    ref\n    id\n    position{\n      ref\n    }\n  }\n}",
           |"variables":{"ref":"${invQty.iqRef}${retId}${invQty.number}","type":"${invQty.type}","catRef":"DEFAULT:${retId}","positionRef":"${invQty.ipRef}${retId}${invQty.ipNumber}","condition":"${invQty.condition}","quantity":${invQty.quantity}},
           |"operationName":"createInventoryQuantity"
           |}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("ipQtyRefs"))
    )}

  val InventoryQuantityForAll = scenario("Inventory Position Creation for All Inventories")
    .asLongAs(_ => InventoryQuantityPositionCount.getAndIncrement() <= totalInventoryPositions) {
      exec(PostInventoryQuantity)
    }
    .exec(session => {
      InventoryQuantityPositionCount = new AtomicInteger(1)
      session
    })

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invQty"-> Carrier.get()))
  }

  val GetInventoryQuantity = {
    feed(queryFeeder())
      .exec(http("Get Inventory Quantities")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  inventoryQuantities(first:${invQty.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.inventoryQuantities.edges[${invQty.getPagination}].cursor").saveAs("iqFirstCursor"))
        .check(jsonPath("$.data.inventoryQuantities.edges[${invQty.getPagination}].node.ref").saveAs("iqRef"))
        .check(jsonPath("$.data.inventoryQuantities.edges[${invQty.getPagination}].node.catalogue.ref").saveAs("iqcatalogueRef"))
      )
  }

  val GetInventoryQuantityByPagination = {
    feed(queryFeeder())
      .exec(http("Get Inventory Quantities By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{"query":"query {\n  inventoryQuantities(${invQty.firstOrLast}:${invQty.pagination} ${invQty.afterOrBefore}:\"${iqFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${invQty.from}\",to:\"${invQty.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetInventoryQuantityById = {
    exec(http("Get Inventory Quantities By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query inventoryQuantity(\n$ref:String!,\n  $iqcatalogueRef:String!\n){\n  inventoryQuantity(ref:$ref catalogue:{ref:$iqcatalogueRef}){\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           }}\n",
          |"variables":{"ref":"${iqRef}","iqcatalogueRef":"${iqcatalogueRef}"},
          |"operationName":"inventoryQuantity"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val InventoryQuantitesQueries = scenario("Controls Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetInventoryQuantity, GetInventoryQuantityByPagination, GetInventoryQuantityById)
    }


}
