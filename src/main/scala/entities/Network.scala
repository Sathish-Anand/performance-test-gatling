package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker


object Network extends Simulation {

  var faker = new Faker();
  val netCount = new AtomicInteger(1)
  val networkCount = netCount.getAndIncrement()
  var networkPrefix = Parameters.stringPrefix
  var networkRef = networkPrefix+"_Net_"

  def get(): Map[String, Any] = {
    Map(
      "name" -> networkRef,
      "networkNumber"-> netCount.getAndIncrement(),
      "type" -> "HD"
    )
  }

  val networkFeeder = Iterator.continually(Map(
    "networkName" -> networkRef,
    "networkNumber"-> netCount.getAndIncrement(),
    "networkType" -> "HD"
  ))

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("Network"-> get()))
  }

  val PostNetwork = {
    feed(networkFeeder)
    .exec(http("Create Network")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """{"query":"\n  mutation{\n  createNetwork(input:{\n name:\"${networkName}${networkNumber}\"\n type: \"${networkType}\"\n retailers:[{\n      id: ${retId}\n    }]\n\n  }) {\n    id\n   ref\n  }\n}"}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.createNetwork.id").saveAs("netId"))
      .check(jsonPath("$.data.createNetwork.ref").saveAs("netRef"))
    )
  }

  val GetNetwork = {
    feed(Carrier.feeder())
      .exec(http("Get Networks")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  networks(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.networks.edges[${feeders.getPagination}].cursor").saveAs("netFirstCursor"))
        .check(jsonPath("$.data.networks.edges[${feeders.getPagination}].node.ref").saveAs("netRef"))
      )
  }

  val GetNetworkByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Network By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  networks(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${netFirstCursor}\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetNetworkById = {
    exec(http("Get Network By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query network(\n$ref:String!\n){\n  network(ref:$ref){\n        id\n        ref\n        type\n             status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${netRef}"},"operationName":"network"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val NetworkQueries = scenario("Network Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetNetwork, GetNetworkByPagination, GetNetworkById)
    }

}
