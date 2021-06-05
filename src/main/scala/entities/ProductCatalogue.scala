package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker

object ProductCatalogue extends Simulation {

  var faker = new Faker();

  def get(): Map[String, Any] = {
    Map(
      "name" -> "testName",
      "type" -> "DEFAULT"
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("prodCat"-> get()))
  }

  val PostProductCatalogue = {
    feed(feeder())
    .exec(http("Create Product Catalogue")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"mutation createProductCatalogue($ref:String!, \n  $retailerName:[String!], $type:String! $name:String!){\n  CP:createProductCatalogue(input:{\n    ref:$ref\n    type: $type\n    name:$name\n    retailerRefs : $retailerName\n     \n  }) {\n    ref\n    type\n    name\n    workflowRef\n    workflowVersion\n    status\n  }\n}",
          |"variables":{"ref":"DEFAULT:${retId}","name":"${prodCat.name}","type":"${prodCat.type}","retailerName":["${retName}"]},
          |"operationName":"createProductCatalogue"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("prodCatRef"))
    )
  }

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("prodCat"-> Carrier.get()))
  }

  val GetProductCatalogue = {
    feed(queryFeeder())
      .exec(http("Get Product Catalogues")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  productCatalogues(first:${prodCat.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n    }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.productCatalogues.edges[0].cursor").saveAs("prodCatFirstCursor"))
        .check(jsonPath("$.data.productCatalogues.edges[0].node.ref").saveAs("prodCatRef"))
      )
  }

  val GetProductCatalogueByPagination = {
    feed(queryFeeder())
      .exec(http("Get Product Catalogue By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  productCatalogues(${prodCat.firstOrLast}:${prodCat.pagination} ${prodCat.afterOrBefore}:\"${prodCatFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${prodCat.from}\",to:\"${prodCat.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetProductCatalogueById = {
    exec(http("Get Product Catalogue By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query productCatalogue(\n$ref:String!\n){\n  productCatalogue(ref:$ref){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${prodCatRef}"},
          |"operationName":"productCatalogue"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val ProductCatalogueQueries = scenario("ProductCatalogue Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetProductCatalogue, GetProductCatalogueByPagination, GetProductCatalogueById)
    }

}
