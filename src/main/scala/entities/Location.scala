package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import scala.util.Random
import io.gatling.http.Predef._
import com.github.javafaker.Faker

object Location extends Simulation {

  var faker = new Faker()
  val rnd = new Random()
  var accountprefix = Parameters.stringPrefix
  var totalLocations = Parameters.location
  val Types = Array("STORE", "WAREHOUSE")
  var locationPrefix = accountprefix.take(3)
  val locationCount = new AtomicInteger(1)

  val locationFeeder = Iterator.continually(Map(
    "name" -> faker.commerce().productName(),
    "type" -> rnd.shuffle(Types.toList).head,
    "loc_ref" -> locationPrefix,
    "number" -> locationCount.getAndIncrement(),
    "company" -> faker.company().name(),
    "ref" -> faker.commerce().material(),
    "latitude" -> faker.address().latitude(),
    "longitude" -> faker.address().longitude(),
    "phonenumber" -> faker.phoneNumber().extension(),
    "locationName" -> "DEMO_location"
    ))

  val PostLocation =
    repeat(totalLocations) {
      feed(locationFeeder())
        .exec(http("Create Location")
          .post("/graphql")
          .headers(header.fluentHeader)
          .body(ElFileBody("data/dataSetup/location.json"))
          .check(status.is(200))
          .check(jsonPath("$.data.createLocation.id").saveAs("locId"))
          .check(jsonPath("$.data.createLocation.ref").saveAs("locRef"))
        )
    }

  val GetLocation = {
    feed(Carrier.feeder())
      .exec(http("Get Locations")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"{\n  locations(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.locations.edges[${feeders.getPagination}].cursor").saveAs("locFirstCursor"))
      .check(jsonPath("$.data.locations.edges[${feeders.getPagination}].node.ref").saveAs("locRef"))
    )
  }

  val GetLocationByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Location By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  locations(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${locFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n        id\n        ref\n        type\n     status\n        createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetLocationById = {
    exec(http("Get location By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query location(\n$ref:String!\n){\n  location(ref:$ref){\n        id\n        ref\n        type\n             status\n        createdOn\n        updatedOn \n      }}\n",
          |"variables":{"ref":"${locRef}"},"operationName":"location"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val LocationQueries = scenario("locations Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetLocation,
//        GetLocationByPagination,
        GetLocationById)
    }

}
