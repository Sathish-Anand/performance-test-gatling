package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.util.concurrent.atomic.AtomicInteger
import com.github.javafaker.Faker
import scala.util.Random
import Product._

object VirtualPosition extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  var users = Configurations.users
  var virtualPositionPrefix = Parameters.stringPrefix
  var vpCount = new AtomicInteger(1)
  var virtualPositionRef = virtualPositionPrefix + "_VP_"
  var parentChildProducts = Product.parentChildProduct
  var noParentProducts = Product.noParentProduct
  var noChildProducts = Product.noChildProduct

  var productCount = new AtomicInteger(1)
  var virtualPositionCount = new AtomicInteger(1)
  var virtualPositionStdCount = new AtomicInteger(1)
  var virtualPositionVarCount = new AtomicInteger(1)
  var totalParentProduct = Parameters.standardProduct
  var totalParentChildProduct = Parameters.standardVariantProduct
  var noOfParentProducts = Parameters.variantOnlyProduct / users
  var noOfChildProducts = Parameters.standardOnlyProduct / users
  var variantProducts = (totalParentProduct * totalParentChildProduct) / users

  var totalNoOfParentProduct = Math.round(noOfParentProducts)
  var totalNoOfChildProduct = Math.round(noOfChildProducts)
  var totalVariantProduct = Math.round(variantProducts)

  def get(): Map[String, Any] = {
    Map(
      "description" -> faker.commerce().promotionCode(),
      "name" -> faker.commerce().productName(),
      "number" -> vpCount.getAndIncrement(),
      "type" -> "DEFAULT",
      "vpRef" -> virtualPositionRef,
      "productRefsStdVar" -> parentChildProducts,
      "productRefsStd" -> noChildProducts,
      "productRefsVar" -> noParentProducts,
      "productRefsNumber" -> productCount.getAndIncrement(),
      "quantity" -> rnd.nextInt(100000)
    )
  }

  def feeder(): Iterator[Map[String, Any]] = {
    Iterator.continually(Map("vPos" -> get()))
  }

  val PostVirtualPositionParentChildProduct = {
    feed(feeder())
      .exec(http("Create Virtual Positions")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createVirtualPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $quantity:Int!){\n    createVirtualPosition(input:{\n      ref: $ref\n      catalogue:{\n        ref:$catRef\n      }\n      type:$type\n      productRef:$productRef\n      quantity:$quantity\n    }\n    ){\n    id\n    ref\n  }\n}",
             |"variables":{"ref":"${vPos.vpRef}${retId}${vPos.number}","type":"${vPos.type}","catRef":"DEFAULT:${retId}","productRef":"${vPos.productRefsStdVar}${retId}${vPos.productRefsNumber}","quantity":${vPos.quantity}},
             |"operationName":"createVirtualPosition"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("vPosRefSV"))
      )
  }

  val PostVirtualPositionParentProduct = {
    feed(feeder())
      .exec(http("Create Virtual Positions")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createVirtualPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $quantity:Int!){\n    createVirtualPosition(input:{\n      ref: $ref\n      catalogue:{\n        ref:$catRef\n      }\n      type:$type\n      productRef:$productRef\n      quantity:$quantity\n    }\n    ){\n    id\n    ref\n  }\n}",
             |"variables":{"ref":"${vPos.vpRef}${retId}${vPos.number}","type":"${vPos.type}","catRef":"DEFAULT:${retId}","productRef":"${vPos.productRefsStd}${retId}${vPos.productRefsNumber}","quantity":${vPos.quantity}},
             |"operationName":"createVirtualPosition"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("vPosRefS"))
      )
  }

  val PostVirtualPositionChildProduct = {
    feed(feeder())
      .exec(http("Create Virtual Positions")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createVirtualPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $quantity:Int!){\n    createVirtualPosition(input:{\n      ref: $ref\n      catalogue:{\n        ref:$catRef\n      }\n      type:$type\n      productRef:$productRef\n      quantity:$quantity\n    }\n    ){\n    id\n    ref\n  }\n}",
             |"variables":{"ref":"${vPos.vpRef}${retId}${vPos.number}","type":"${vPos.type}","catRef":"DEFAULT:${retId}","productRef":"${vPos.productRefsVar}${retId}${vPos.productRefsNumber}","quantity":${vPos.quantity}},
             |"operationName":"createVirtualPosition"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("vPosRefV"))
      )
  }


  val virtualPositionChildProducts = scenario("Create Virtual Positions For Variant Products With Parent Product")
    .asLongAs(_ => virtualPositionCount.getAndIncrement() <= totalVariantProduct) {
      exec(PostVirtualPositionParentChildProduct)
    }
    .exec(session => {
      virtualPositionCount = new AtomicInteger(1)
      productCount = new AtomicInteger(1)
      session
    })

  val virtualPositionStdParentProducts = scenario("Create Virtual Positions For Standard Products Without Child")
    .asLongAs(_ => virtualPositionStdCount.getAndIncrement() <= totalNoOfChildProduct) {
      exec(PostVirtualPositionParentProduct)
    }
    .exec(session => {
      virtualPositionStdCount = new AtomicInteger(1)
      productCount = new AtomicInteger(1)
      session
    })

  val virtualPositionVarParentProducts = scenario("Create Virtual Positions For Variant Products Without Parent")
    .asLongAs(_ => virtualPositionVarCount.getAndIncrement() <= totalNoOfParentProduct) {
      exec(PostVirtualPositionChildProduct)
    }
    .exec(session => {
      virtualPositionVarCount = new AtomicInteger(1)
      productCount = new AtomicInteger(1)
      session
    })

  val GetVirtualPosition = {
    feed(Carrier.feeder())
      .exec(http("Get Virtual Positions")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  virtualPositions(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.virtualPositions.edges[${feeders.getPagination}].cursor").saveAs("vpFirstCursor"))
        .check(jsonPath("$.data.virtualPositions.edges[${feeders.getPagination}].node.ref").saveAs("vpRef"))
        .check(jsonPath("$.data.virtualPositions.edges[${feeders.getPagination}].node.catalogue.ref").saveAs("vpcatalogueRef"))
      )
  }

  val GetVirtualPositionByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Virtual Positions By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  virtualPositions(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${vpFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetVirtualPositionById = {
    exec(http("Get Virtual Positions By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query virtualPosition(\n$ref:String!,\n  $vpcatalogueRef:String!\n){\n  virtualPosition(ref:$ref catalogue:{ref:$vpcatalogueRef}){\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n           }}\n",
          |"variables":{"ref":"${vpRef}","vpcatalogueRef":"${vpcatalogueRef}"},
          |"operationName":"virtualPosition"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }
  val VirtualPositionQueries = scenario("Virtual Positions Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetVirtualPosition, GetVirtualPositionByPagination, GetVirtualPositionById)
    }
}
