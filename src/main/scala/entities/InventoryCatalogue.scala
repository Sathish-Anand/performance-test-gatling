package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker

object InventoryCatalogue extends Simulation {

  var faker = new Faker();

  def get(): Map[String, Any] = {
    Map(
      "name" -> "Test Name",
      "type" -> "DEFAULT",
      "description" -> "Test Description"
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invCat"-> get()))
  }

  val PostInventoryCatalogue = {
    feed(feeder())
    .exec(http("Create Inventory Catalogue")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"mutation createInventoryCatalogue($ref:String!, \n  $retailerName:[String!], $type:String! $name:String! $description:String!){\n  IC:createInventoryCatalogue(input:{\n    ref:$ref\n    type:$type\n    name:$name\n    description:$description\n    retailerRefs:$retailerName\n  }){\n    id\n    ref\n    type\n    status\n    name\n    description\n    retailerRefs\n  }\n}",
          |"variables":{"ref":"DEFAULT:${retId}","name":"${invCat.name}","description":"${invCat.description}","type":"${invCat.type}","retailerName":["${retName}"]},
          |"operationName":"createInventoryCatalogue"
          |}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("invCatRef"))
    )
  }

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("invCat"-> Carrier.get()))
  }

  val GetInventoryCatalogue = {
    feed(queryFeeder())
      .exec(http("Get Inventory Catalogue")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  inventoryCatalogues(first:${invCat.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n    }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.inventoryCatalogues.edges[0].cursor").saveAs("invCatFirstCursor"))
        .check(jsonPath("$.data.inventoryCatalogues.edges[0].node.ref").saveAs("invCatRef"))
      )
  }

  val GetInventoryCatalogueByPagination = {
    feed(queryFeeder())
      .exec(http("Get Inventory Catalogue By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  inventoryCatalogues(${invCat.firstOrLast}:${invCat.pagination} ${invCat.afterOrBefore}:\"${invCatFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${invCat.from}\",to:\"${invCat.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetInventoryCatalogueById = {
    exec(http("Get Inventory Catalogue By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query inventoryCatalogue(\n$ref:String!\n){\n  inventoryCatalogue(ref:$ref){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${invCatRef}"},
          |"operationName":"inventoryCatalogue"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val InventoryCatalogueQueries = scenario("Inventory Catalogue Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetInventoryCatalogue, GetInventoryCatalogueByPagination, GetInventoryCatalogueById)
    }

}
