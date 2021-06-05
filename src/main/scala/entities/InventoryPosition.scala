package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario._
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.util.concurrent.atomic.AtomicInteger
import com.github.javafaker.Faker
import scala.util.Random

object InventoryPosition extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  var users = Configurations.users
  var ipCount = new AtomicInteger(1)
  var productCount = new AtomicInteger(1)
  var locationCount = new AtomicInteger(1)
  var InventoryQuantityNumberCount = new AtomicInteger(1)
  var InventoryPositionProductCount = new AtomicInteger(1)
  var InventoryPositionLocationCount = new AtomicInteger(1)

  var inventoryPositionPrefix = Parameters.stringPrefix
  var inventoryPositionRef = inventoryPositionPrefix + "_IP_"
  var InventoryQuantityRef = inventoryPositionPrefix + "_IQ_"

  var noChildProducts = Product.noChildProduct
  var noParentProducts = Product.noParentProduct
  var locationPrefixRef = Location.locationPrefix
  var parentChildProducts = Product.parentChildProduct
  var totalStandardProduct = Parameters.standardProduct
  var totalVariantProduct = Parameters.standardVariantProduct

  var variantOnlyProducts = Parameters.variantOnlyProduct/users
  var standardOnlyProducts = Parameters.standardOnlyProduct/users
  var variantStandardProducts = (totalStandardProduct * totalVariantProduct)/users
  var locations = (Parameters.network * Parameters.location)/users

  var totalVariantOnlyProduct = Math.round(variantOnlyProducts)+1
  var totalStandardOnlyProduct = Math.round(standardOnlyProducts)+1
  var totalVariantStandardProduct = Math.round(variantStandardProducts)+1
  var totalLocations = Math.round(locations)+1

  println("1."+totalVariantOnlyProduct+","+totalStandardOnlyProduct+","+totalVariantStandardProduct+","+totalLocations)

  def get(): Map[String, Any] = {
    Map(
      "description" -> faker.commerce().promotionCode(),
      "name" -> faker.commerce().productName(),
      "number" -> ipCount.getAndIncrement(),
      "qtyNumber" -> InventoryQuantityNumberCount.getAndIncrement(),
      "type" -> "DEFAULT",
      "condition" -> "NEW",
      "ipRef" -> inventoryPositionRef,
      "iqRef" -> InventoryQuantityRef,
      "stdVarProductRefs" -> parentChildProducts,
      "productRefsStd" -> noChildProducts,
      "productRefsVar" -> noParentProducts,
      "quantity" -> rnd.nextInt(100000)
    )}

  def getLoc(): Map[String, Any] = {
    Map(
      "locationRefs" -> locationPrefixRef,
      "locationRefsNumber" -> locationCount.getAndIncrement()
    )}

  def getProd(): Map[String, Any] = {
    Map(
      "productRefsNumber" -> productCount.getAndIncrement()
    )}

  def prodFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invPosProd"-> getProd()))
  }

  def locationFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invPosLoc"-> getLoc()))
  }

  def inventoryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invPos"-> get()))
  }

  val PostInventoryPositionParentChildProduct = {
    feed(inventoryFeeder())
      .exec(http("Create Inventory Positions For Variant Products with Standard Product")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $locationRef:String! $quantity:Int!){\n createInventoryPosition(input: {\n  ref: $ref\n  catalogue: {ref: $catRef }\n  type: $type \n  onHand: $quantity\n  productRef: $productRef\n    locationRef: $locationRef\n}) {\n    id\n    ref\n    productRef\n    locationRef\n  }\n}\n",
             |"variables":{"ref":"${invPos.ipRef}${retId}${invPos.qtyNumber}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","locationRef":"${invPosLoc.locationRefs}${invPosLoc.locationRefsNumber}","productRef":"${invPos.stdVarProductRefs}${retId}${invPosProd.productRefsNumber}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryPosition"}
             |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipPosRefSV1"))
      )
      .exec(http("Create Inventory Quantity Inventory Positions For Variant Products with Standard Product")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryQuantity($ref:String!, \n  $type:String! $catRef:String! $positionRef:String! $condition:String! $quantity:Int!){\n    createInventoryQuantity(input:{\n   ref:$ref\n    catalogue:{\n      ref:$catRef\n    }\n    position:{\n      ref:$positionRef\n      catalogue:{\n        ref:$catRef\n      }\n    }\n    type:$type\n    quantity:$quantity\n    condition:$condition\n  }){\n    ref\n    id\n    position{\n      ref\n    }\n  }\n}",
             |"variables":{"ref":"${invPos.iqRef}${retId}${invPos.number}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","positionRef":"${ipPosRefSV1}","condition":"${invPos.condition}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryQuantity"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipQtyRefs1"))
      )
  }

  val PostInventoryPositionParentProduct = {
    feed(inventoryFeeder())
      .exec(http("Create Inventory Positions For Standard Product Without Child Products")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $locationRef:String! $quantity:Int!){\n createInventoryPosition(input: {\n  ref: $ref\n  catalogue: {ref: $catRef }\n  type: $type \n  onHand: $quantity\n  productRef: $productRef\n    locationRef: $locationRef\n}) {\n    id\n    ref\n    productRef\n    locationRef\n  }\n}\n",
             |"variables":{"ref":"${invPos.ipRef}${retId}${invPos.number}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","locationRef":"${invPosLoc.locationRefs}${invPosLoc.locationRefsNumber}","productRef":"${invPos.productRefsStd}${retId}${invPosProd.productRefsNumber}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryPosition"}
             |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipPosRefS2"))
      )
      .exec(http("Create Inventory Quantity for Inventory Positions For Standard Product Without Child Products")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryQuantity($ref:String!, \n  $type:String! $catRef:String! $positionRef:String! $condition:String! $quantity:Int!){\n    createInventoryQuantity(input:{\n   ref:$ref\n    catalogue:{\n      ref:$catRef\n    }\n    position:{\n      ref:$positionRef\n      catalogue:{\n        ref:$catRef\n      }\n    }\n    type:$type\n    quantity:$quantity\n    condition:$condition\n  }){\n    ref\n    id\n    position{\n      ref\n    }\n  }\n}",
             |"variables":{"ref":"${invPos.iqRef}${retId}${invPos.number}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","positionRef":"${ipPosRefS2}","condition":"${invPos.condition}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryQuantity"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipQtyRefs2"))
      )
  }

  val PostInventoryPositionChildProduct = {
    feed(inventoryFeeder())
      .exec(http("Create Inventory Positions For Variant Products without Parent Product")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryPosition($ref:String!, \n  $type:String! $catRef:String! $productRef:String! $locationRef:String! $quantity:Int!){\n createInventoryPosition(input: {\n  ref: $ref\n  catalogue: {ref: $catRef }\n  type: $type \n  onHand: $quantity\n  productRef: $productRef\n    locationRef: $locationRef\n}) {\n    id\n    ref\n    productRef\n    locationRef\n  }\n}\n",
             |"variables":{"ref":"${invPos.ipRef}${retId}${invPos.number}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","locationRef":"${invPosLoc.locationRefs}${invPosLoc.locationRefsNumber}","productRef":"${invPos.productRefsVar}${retId}${invPosProd.productRefsNumber}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryPosition"}
             |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipPosRefS3"))
      )
      .exec(http("Create Inventory Quantity for Inventory Positions For Variant Products without Parent Product")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{
             |"query":"mutation createInventoryQuantity($ref:String!, \n  $type:String! $catRef:String! $positionRef:String! $condition:String! $quantity:Int!){\n    createInventoryQuantity(input:{\n   ref:$ref\n    catalogue:{\n      ref:$catRef\n    }\n    position:{\n      ref:$positionRef\n      catalogue:{\n        ref:$catRef\n      }\n    }\n    type:$type\n    quantity:$quantity\n    condition:$condition\n  }){\n    ref\n    id\n    position{\n      ref\n    }\n  }\n}",
             |"variables":{"ref":"${invPos.iqRef}${retId}${invPos.number}","type":"${invPos.type}","catRef":"DEFAULT:${retId}","positionRef":"${ipPosRefS3}","condition":"${invPos.condition}","quantity":${invPos.quantity}},
             |"operationName":"createInventoryQuantity"
             |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("ipQtyRefs3"))
      )
  }

  val InventoryPositionLocationBasedOnSPVP =
    asLongAs(_ => InventoryPositionLocationCount.getAndIncrement() <= totalLocations) {
      feed(locationFeeder())
        .exec(PostInventoryPositionParentChildProduct)
    }
      .exec(session => {
        InventoryPositionLocationCount = new AtomicInteger(1)
//        locationCount = new AtomicInteger(1)
        session
      })

  val InventoryPositionLocationBasedOnSP =
    asLongAs(_ => InventoryPositionLocationCount.getAndIncrement() <= totalLocations) {
      feed(locationFeeder())
        .exec(PostInventoryPositionParentProduct)
    }
      .exec(session => {
        InventoryPositionLocationCount = new AtomicInteger(1)
//        locationCount = new AtomicInteger(1)
        session
      })

  val InventoryPositionLocationBasedOnVP =
    asLongAs(_ => InventoryPositionLocationCount.getAndIncrement() <= totalLocations) {
      feed(locationFeeder())
        .exec(PostInventoryPositionChildProduct)
    }
      .exec(session => {
        InventoryPositionLocationCount = new AtomicInteger(1)
//        locationCount = new AtomicInteger(1)
        session
      })

  val InventoryPositionStandardVariantProducts = scenario("Inventory Position Creation for Standard-Variant Products")
    .asLongAs(_ => InventoryPositionProductCount.getAndIncrement() <= totalVariantStandardProduct) {
      feed(prodFeeder())
        .exec(InventoryPositionLocationBasedOnSPVP)
    }
    .exec(session => {
      InventoryPositionProductCount = new AtomicInteger(1)
//      productCount = new AtomicInteger(1)
      session
    })

  val InventoryPositionStandardOnlyProducts = scenario("Inventory Position Creation for Standard Only Products")
    .asLongAs(_ => InventoryPositionProductCount.getAndIncrement() <= totalStandardOnlyProduct) {
      feed(prodFeeder())
        .exec(InventoryPositionLocationBasedOnSP)
    }
    .exec(session => {
      InventoryPositionProductCount = new AtomicInteger(1)
//      productCount = new AtomicInteger(1)
      session
    })

  val InventoryPositionVariantOnlyProducts = scenario("Inventory Position Creation for Variant Only Products")
    .asLongAs(_ => InventoryPositionProductCount.getAndIncrement() <= totalVariantOnlyProduct) {
      feed(prodFeeder())
        .exec(InventoryPositionLocationBasedOnVP)
    }
    .exec(session => {
      InventoryPositionProductCount = new AtomicInteger(1)
//      productCount = new AtomicInteger(1)
      session
    })

}
