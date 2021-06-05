package entities

import java.util.concurrent.atomic.AtomicInteger

import com.github.javafaker.Faker
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import util.{Configurations, SetupParameters => Parameters}

import scala.util.Random

object ProductQuery extends Simulation {

  var faker = new Faker()
  val rnd = new Random()


  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("products"-> Carrier.get()))
  }

  val GetVariantProduct = {
    feed(queryFeeder())
      .exec(http("Get Variant Products")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  variantProducts(first:${products.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n        categories{\n          edges{\n            node{\n              id\n              ref\n            }\n          }\n        }\n       \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.variantProducts.edges[${products.getPagination}].cursor").saveAs("vpFirstCursor"))
        .check(jsonPath("$.data.variantProducts.edges[${products.getPagination}].node.ref").saveAs("vpRef"))
        .check(jsonPath("$.data.variantProducts.edges[${products.getPagination}].node.catalogue.ref").saveAs("catalogueRef"))
      )
  }

  val GetVariantProductByPagination = {
    feed(queryFeeder())
      .exec(http("Get Variant Products By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{"query":"query {\n  variantProducts(${products.firstOrLast}:${products.pagination} ${products.afterOrBefore}:\"${vpFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${products.from}\",to:\"${products.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetVariantProductById = {
    exec(http("Get Variant Products By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query variantProduct(\n$ref:String!,\n  $catalogueRef:String!\n){\n  variantProduct(ref:$ref catalogue:{ref:$catalogueRef}){\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n        categories{\n          edges{\n            node{\n              id\n              ref\n            }\n          }\n        }\n      }}\n",
          |"variables":{"ref":"${vpRef}","catalogueRef":"${catalogueRef}"},
          |"operationName":"standardProduct"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val VariantProductQueries = scenario("Variant Products Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(
//        GetVariantProduct,
        GetVariantProductByPagination, GetVariantProductById)
    }

  val GetStandardProduct = {
    feed(queryFeeder())
      .exec(http("Get Standard Products")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  standardProducts(first:${products.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n        categories{\n          edges{\n            node{\n              id\n              ref\n            }\n          }\n        }\n       \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.standardProducts.edges[${products.getPagination}].cursor").saveAs("spFirstCursor"))
        .check(jsonPath("$.data.standardProducts.edges[${products.getPagination}].node.ref").saveAs("spRef"))
        .check(jsonPath("$.data.standardProducts.edges[${products.getPagination}].node.catalogue.ref").saveAs("catalogueRef"))
      )
  }

  val GetStandardProductByPagination = {
    feed(queryFeeder())
      .exec(http("Get Standard Products By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{"query":"query {\n  standardProducts(${products.firstOrLast}:${products.pagination} ${products.afterOrBefore}:\"${spFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${products.from}\",to:\"${products.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetStandardProductById = {
    exec(http("Get Standard Products By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query standardProduct(\n$ref:String!,\n  $catalogueRef:String!\n){\n  standardProduct(ref:$ref catalogue:{ref:$catalogueRef}){\n        id\n        ref\n        type\n        status\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n        catalogue{\n          ref\n        }\n        categories{\n          edges{\n            node{\n              id\n              ref\n            }\n          }\n        }\n      }}\n",
          |"variables":{"ref":"${spRef}","catalogueRef":"${catalogueRef}"},
          |"operationName":"standardProduct"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val StandProductQueries = scenario("Standard Products Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(
//        GetStandardProduct,
        GetStandardProductByPagination, GetStandardProductById)
    }

}
