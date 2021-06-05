package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.util.concurrent.atomic.AtomicInteger
import com.github.javafaker.Faker

object VirtualCatalogue extends Simulation {

  var faker = new Faker();
  var virtualCataloguePrefix = Parameters.stringPrefix
  var vcCount = new AtomicInteger(1)
  var virtualCatalogueRef = virtualCataloguePrefix+ "_VC_"

  def get(): Map[String, Any] = {
    Map(
      "description" -> faker.commerce().promotionCode(),
      "name" -> "Test Virtual Catalogue Name",
      "number" -> vcCount.getAndIncrement(),
      "type" -> "BASE",
      "vcRef" -> virtualCatalogueRef
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("vcCat"-> get()))
  }

  val PostVirtualCatalogue = {
    feed(feeder())
    .exec(http("Create Virtual Catalogue")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"mutation createVirtualCatalogue($ref:String!, \n  $retailerName:[String!], $type:String! $name:String! $invRef:String!,$pcRef:String! ){\n  VC: createVirtualCatalogue(input: {\n    ref: $ref \n    type: $type\n    inventoryCatalogueRef:$invRef\n    productCatalogueRef: $pcRef\n    name: $name\n    retailerRefs: $retailerName\n  }) {\n   id\n    ref\n     type\n    name\n    workflowRef\n    workflowVersion\n    status\n  }\n}\n",
          |"variables":{"ref":"DEFAULT:${retId}","name":"${vcCat.name}","invRef":"${invCatRef}","pcRef":"${prodCatRef}","type":"${vcCat.type}","retailerName":["${retName}"]},
          |"operationName":"createVirtualCatalogue"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.VC.ref").saveAs("vcCatRef"))
    )
  }

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("virtualCat"-> Carrier.get()))
  }

  val GetVirtualCatalogue = {
    feed(queryFeeder())
      .exec(http("Get Virtual Catalogues")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  virtualCatalogues(first:${virtualCat.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n    }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.virtualCatalogues.edges[0].cursor").saveAs("vrtCatFirstCursor"))
        .check(jsonPath("$.data.virtualCatalogues.edges[0].node.ref").saveAs("vrtCatRef"))
      )
  }

  val GetVirtualCatalogueByPagination = {
    feed(queryFeeder())
      .exec(http("Get Virtual Catalogue By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  virtualCatalogues(${virtualCat.firstOrLast}:${virtualCat.pagination} ${virtualCat.afterOrBefore}:\"${vrtCatFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${virtualCat.from}\",to:\"${virtualCat.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetVirtualCatalogueById = {
    exec(http("Get Virtual Catalogue By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query virtualCatalogue(\n$ref:String!\n){\n  virtualCatalogue(ref:$ref){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${vrtCatRef}"},
          |"operationName":"virtualCatalogue"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val VirtualCatalogueQueries = scenario("Virtual Catalogue Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetVirtualCatalogue, GetVirtualCatalogueByPagination, GetVirtualCatalogueById)
    }

}
