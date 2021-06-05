package entities

import java.util.Random

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._

import com.github.javafaker.Faker

object StoreAddress extends Simulation {

  val rnd = new Random();
  val faker = new Faker();
  val r = new scala.util.Random(31)

  val newStoreAddressFeeder = Iterator.continually(Map(
    "ref" -> r.nextString(6),
    "country" -> faker.address().country(),
    "city" -> faker.address().city(),
    "name" -> faker.address().firstName(),
    "state" -> faker.address().state(),
    "postcode" -> faker.address().zipCode(),
    "longitude" -> faker.address().longitude(),
    "latitude" -> faker.address().latitude()
  ))

  val updateStoreAddressFeeder = Iterator.continually(Map(
    "comnayName" -> faker.address().firstName()
  ))

  val CreateStoreAddress = {
    feed(newStoreAddressFeeder)
    .exec(http("Create Store Address")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateStoreAddressInput!) {\n  createStoreAddress(input: $input) {\n    id\n    ref\n    street\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","name":"${name}","city":"${city}","country":"${country}","state":"${state}","postcode":"${postcode}","longitude":${longitude},"latitude":${latitude}}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createStoreAddress.id").saveAs("storeAddessId"))
      )
  }

  val UpdateStoreAddress = {
    feed(updateStoreAddressFeeder)
      .exec(http("Update Store Address")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateStoreAddressInput!) {\n  updateStoreAddress(input: $input) {\n    id\n    ref\n    street\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${storeAddessId},"companyName":"${comnayName}"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetStoreAddress = {
    feed(Carrier.feeder())
      .exec(http("Get Store Addresses")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  storeAddresses(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        createdOn\n        updatedOn \n        street\n        region\n        country\n        latitude\n        longitude\n        directions   \n      }\n      cursor\n    }\n  }\n}"}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.storeAddresses.edges[${feeders.getPagination}].cursor").saveAs("sasFirstCursor"))
        .check(jsonPath("$.data.storeAddresses.edges[${feeders.getPagination}].node.id").saveAs("sasId"))
      )
  }

  val GetStoreAddressByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Store Address By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  storeAddresses(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${sasFirstCursor}\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n     id\n        ref\n        type\n        createdOn\n        updatedOn \n        street\n        region\n        country\n        latitude\n        longitude\n        directions   \n      }\n      cursor\n    }\n  }\n}"}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetStoreAddressById = {
    exec(http("Get Store Address By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query storeAddressById(\n$id:ID!\n){\n  storeAddressById(id:$id)\n  {\n         id\n        ref\n        type\n        createdOn\n        updatedOn \n        street\n        region\n        country\n        latitude\n        longitude\n        directions \n     }\n}\n",
          |"variables":{"id":"${sasId}"},
          |"operationName":"storeAddressById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }
  val StoreAddressQueries = scenario("Store Addresses Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetStoreAddress, GetStoreAddressByPagination, GetStoreAddressById)
    }
}
