package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.util.Random

object Control extends Simulation {

  val rnd = new Random()
  val entityPrefix: String = Parameters.stringPrefix

  val CreateControlGroup = {
    feed(Iterator.continually(Map(
      "ref" -> entityPrefix.concat("-").concat(rnd.nextInt(100000000).toString)
    ))).exec(http("Create Control Group")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """{"query":"mutation($input: CreateControlGroupInput!) {\n  createControlGroup(input: $input) {\n    id\n    ref\n    createdOn\n  }\n}","variables":{"input":{"ref":"CG-${ref}","type":"DEFAULT","name":"CG-Name-${ref}"}}}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.createControlGroup.id").saveAs("controlGroupId"))
      .check(jsonPath("$.data.createControlGroup.ref").saveAs("controlGroupRef"))
    )
  }

  val CreateControl = {
    feed(Iterator.continually(Map(
      "controlRef" -> entityPrefix.concat("-").concat(String.valueOf(rnd.nextInt(100000000)))
    ))).exec(http("Create Control")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """{"query":"mutation($input: CreateControlInput!) {\n  createControl(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${controlRef}","controlGroup":{"ref":"${controlGroupRef}"},"type":"DEFAULT","name":"${controlRef}"}}}""".stripMargin))
      .check(status.is(200))
    )
  }

  val UpdateControl = {
    exec(http("Update Control")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """{"query":"mutation($input: UpdateControlInput!) {\n  updateControl(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${controlRef}","description":"Updated"}}}""".stripMargin))
      .check(status.is(200))
    )
  }

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("controls"-> Carrier.get()))
  }

  val GetControl = {
    feed(queryFeeder())
      .exec(http("Get controls")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"{\n  controls(first:${controls.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n controlGroup{\n          ref\n        }\n     }\n      cursor\n    }\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.controls.edges[${controls.getPagination}].cursor").saveAs("ctrlFirstCursor"))
      .check(jsonPath("$.data.controls.edges[${controls.getPagination}].node.ref").saveAs("ctrlRef"))
        .check(jsonPath("$.data.controls.edges[${controls.getPagination}].node.controlGroup.ref").saveAs("ctrlGroupRef"))
    )
  }

  val GetControlByPagination = {
    feed(queryFeeder())
      .exec(http("Get Control By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query Control($first:Int,\n  $after:String,\n  $from:DateTime,\n  $to:DateTime \n){\n  controls(first:$first after:$after status:\"CREATED\" createdOn:{from:$from,to:$to} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n       }\n      cursor\n    }\n  }\n}",
            |"variables":{"${controls.firstOrLast}":${controls.pagination}, "${controls.afterOrBefore}":"${ctrlFirstCursor}","from":"${controls.from}","to":"${controls.to}"},
            |"operationName":"Control"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetControlById = {
    exec(http("Get Control By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query Control(\n$ref:String!,\n  $groupRef:String!\n){\n  control(ref:$ref controlGroup:{ref:$groupRef}){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${ctrlRef}","groupRef":"${ctrlGroupRef}"},
          |"operationName":"Control"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val ControlQueries = scenario("Controls Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetControl, GetControlByPagination, GetControlById)
    }
}
