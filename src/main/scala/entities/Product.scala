package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import com.github.javafaker.Faker
import scala.util.Random

object Product extends Simulation {

  var faker = new Faker()
  val rnd = new Random()

  var parentChildProductCount = new AtomicInteger(1)
  var parentProductCount = new AtomicInteger(1)
  var noOfParentProductCount = new AtomicInteger(1)
  var noOfChildProductCount = new AtomicInteger(1)
  var stdCount = new AtomicInteger(1)

  var productPrefix = Parameters.stringPrefix
  var parentChildProduct = productPrefix+"_Std_Var_"
  var noParentProduct = productPrefix+"_Var_"
  var parentProduct = productPrefix+"_Std_"
  var noChildProduct = productPrefix+"_StdOnly_"
  var category = productPrefix+"_Cat_"

  var users = Configurations.users
  var totalCategory = (Parameters.category/2) - 1
  val catRange = List.range(1,totalCategory)
  var parentProducts = Parameters.standardProduct/users
  var parentChildProducts = Parameters.standardVariantProduct/users
  var noOfParentProducts = Parameters.variantOnlyProduct/users
  var noOfChildProducts = Parameters.standardOnlyProduct/users


  var totalParentProduct = Math.round(parentProducts)+1
  var totalParentChildProduct = Math.round(parentChildProducts)+1
  var totalNoOfParentProduct = Math.round(noOfParentProducts)+1
  var totalNoOfChildProduct = Math.round(noOfChildProducts)+1


  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "standardRef" -> parentProduct,
      "standardVariantRef" -> parentChildProduct,
      "standardOnlyRef" -> noChildProduct,
      "variantOnlyRef" -> noParentProduct,
      "standardNumber" -> parentProductCount.getAndIncrement(),
      "standardVariantNumber" -> parentChildProductCount.getAndIncrement(),
      "noChildNumber" -> noOfChildProductCount.getAndIncrement(),
      "noParentNumber" -> noOfParentProductCount.getAndIncrement(),
      "type" -> "DEFAULT",
      "catRef" -> category,
      "catRefRange" -> (rnd.nextInt(totalCategory)+1)
//      "catRefRange" -> rnd.shuffle(catRange.toList).head
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("product"-> get()))
  }

  val PostStandardProduct =
  {
    feed(feeder())
    .exec(http("Create Standard Product")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"mutation createStandardProduct($productRef:String!, $productName:String!, $type:String! $catalogueRef:String! $categoryRef:String!){\n  createStandardProduct(input:{\n     ref:$productRef\n    name:$productName\n    catalogue:{\n      ref:$catalogueRef\n    }\n    type:$type\n     \n    categories:{\n      ref:$categoryRef\n      catalogue:{\n        ref:$catalogueRef\n      }\n    }\n    prices:[\n      {\n        type: \"CURRENT\",\n        value: 59.95,\n        currency: \"AUD\"\n      },\n      {\n        type: \"ORIGINAL\",\n        value: 39.95,\n        currency: \"AUD\"\n      }\n    ]\n    gtin:\"SAT0123\"\n    taxType:{\n      country:\"AUSTRALIA\"\n      group:\"SHIRT\"\n      tariff:\"VARIANT\"\n    }\n \n  }){\n    id\n    ref\n    gtin\n    summary\n    type\n    status\n  }\n}",
          |"variables":{"productRef":"${product.standardRef}${retId}${product.standardNumber}","productName":"${product.name}","catalogueRef":"DEFAULT:${retId}","type":"${product.type}","categoryRef":"${product.catRef}${retId}${product.catRefRange}"},
          |"operationName":"createStandardProduct"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("stdProdRef"))
    )
  }

  val PostVariantProductWithStandard =
    repeat(totalParentChildProduct){
      feed(feeder())
        .exec(http("Create Variant Product")
          .post("/graphql")
          .headers(header.fluentHeader)
          .body(StringBody(
            """
              |{
              |"query":"mutation createVariantProduct($productRef:String!, $productName:String!, $type:String! $stdProdRef:String! $catalogueRef:String! $categoryRef:String!){\n  createVariantProduct(input:{\n     ref:$productRef\n    name:$productName\n    catalogue:{\n      ref:$catalogueRef\n    }\n    type:$type\n     \n    categories:{\n      ref:$categoryRef\n      catalogue:{\n        ref:$catalogueRef\n      }\n    }\n        product:{\n      ref:$stdProdRef\n      catalogue:{\n        ref:$catalogueRef\n      }\n    }\n    prices:[\n      {\n        type: \"CURRENT\",\n        value: 59.95,\n        currency: \"AUD\"\n      },\n      {\n        type: \"ORIGINAL\",\n        value: 39.95,\n        currency: \"AUD\"\n      }\n    ]\n    gtin:\"SAT0123\"\n    taxType:{\n      country:\"AUSTRALIA\"\n      group:\"SHIRT\"\n      tariff:\"VARIANT\"\n    }\n \n  }){\n    id\n    ref\n    gtin\n    summary\n    type\n    status\n  }\n}",
              |"variables":{"productRef":"${product.standardVariantRef}${retId}${product.standardVariantNumber}","stdProdRef":"${stdProdRef}","productName":"${product.name}","catalogueRef":"DEFAULT:${retId}","type":"${product.type}","categoryRef":"${product.catRef}${retId}${product.catRefRange}"},
              |"operationName":"createVariantProduct"
              |}
              |""".stripMargin))
          .check(status.is(200))
          .check(jsonPath("$.data..ref").saveAs("varProdRef"))
        )
    }

  val PostStandardProductWithNoChild =
    repeat(totalNoOfChildProduct){
    feed(feeder())
      .exec(http("Create Standard Product Only")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"mutation createStandardProduct($productRef:String!, $productName:String!, $type:String! $catalogueRef:String! $categoryRef:String!){\n  createStandardProduct(input:{\n ref:$productRef\n    name:$productName\n    catalogue:{\n      ref:$catalogueRef\n    }\n    type:$type\n     \n    categories:{\n      ref:$categoryRef\n      catalogue:{\n        ref:$catalogueRef\n      }\n    }\n    prices:[\n      {\n        type: \"CURRENT\",\n        value: 59.95,\n        currency: \"AUD\"\n      },\n      {\n        type: \"ORIGINAL\",\n        value: 39.95,\n        currency: \"AUD\"\n      }\n    ]\n    gtin:\"SAT0123\"\n    taxType:{\n      country:\"AUSTRALIA\"\n      group:\"SHIRT\"\n      tariff:\"VARIANT\"\n    }\n \n  }){\n    id\n    ref\n    gtin\n    summary\n    type\n    status\n  }\n}",
            |"variables":{"productRef":"${product.standardOnlyRef}${retId}${product.noChildNumber}","productName":"${product.name}","catalogueRef":"DEFAULT:${retId}","type":"${product.type}","categoryRef":"${product.catRef}${retId}${product.catRefRange}"},
            |"operationName":"createStandardProduct"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("stdOnlyProdRef"))
      )
  }

  val PostVariantProductWithNoParent =
    repeat(totalNoOfParentProduct){
    feed(feeder())
      .exec(http("Create Variant Product Only")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"mutation createVariantProduct($productRef:String!, $productName:String!, $type:String! $catalogueRef:String! $categoryRef:String!){\n  createVariantProduct(input:{\n  ref:$productRef\n    name:$productName\n    catalogue:{\n      ref:$catalogueRef\n    }\n    type:$type\n    categories:{\n      ref:$categoryRef\n      catalogue:{\n        ref:$catalogueRef\n      }\n    }\n    prices:[\n      {\n        type: \"CURRENT\",\n        value: 59.95,\n        currency: \"AUD\"\n      },\n      {\n        type: \"ORIGINAL\",\n        value: 39.95,\n        currency: \"AUD\"\n      }\n    ]\n    gtin:\"SAT0123\"\n    taxType:{\n      country:\"AUSTRALIA\"\n      group:\"SHIRT\"\n      tariff:\"VARIANT\"\n    }\n \n  }){\n    id\n    ref\n    gtin\n    summary\n    type\n    status\n  }\n}",
            |"variables":{"productRef":"${product.variantOnlyRef}${retId}${product.noParentNumber}","productName":"${product.name}","catalogueRef":"DEFAULT:${retId}","type":"${product.type}","categoryRef":"${product.catRef}${retId}${product.catRefRange}"},
            |"operationName":"createVariantProduct"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data..ref").saveAs("varOnlyProdRef"))
      )
  }

  val rootChildProduct = scenario("Standard Variant Product")
    .asLongAs(_ => stdCount.getAndIncrement() <= totalParentProduct) {
      exec(PostStandardProduct, PostVariantProductWithStandard)
    }


  val standardVariantProductSetup = scenario("Standard Variant Product Setup")
    .exec(rootChildProduct, PostStandardProductWithNoChild, PostVariantProductWithNoParent)
    .exec(session => {
//      parentProductCount = new AtomicInteger(1)
//      parentChildProductCount = new AtomicInteger(1)
//      noOfParentProductCount = new AtomicInteger(1)
//      noOfChildProductCount = new AtomicInteger(1)
//      stdCount = new AtomicInteger(1)
      println("Current Value is " + parentProductCount)
      session
      }
    )

}
