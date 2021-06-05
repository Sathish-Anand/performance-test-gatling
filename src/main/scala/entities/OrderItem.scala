package entities

import util.{SetupParameters => Parameters}
import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import com.github.javafaker.Faker
import scala.util.Random

object OrderItem extends Simulation {

  val rnd = new Random()
  val faker = new Faker();
  val orderItemPrefix: String = Parameters.stringPrefix

  val orderItemFeeder = Iterator.continually(Map(
    "ref" -> orderItemPrefix.concat("-").concat(rnd.nextInt(1000000).toString),
    "productRef" -> faker.commerce().material(),
    "quantity" -> faker.number().randomDigit()
  ))

  val CreateOrderItem = {
    feed(orderItemFeeder)
    .exec(http("Create Order Item")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateOrderItemInput!) {\n  createOrderItem(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","productRef":"${productRef}","productCatalogueRef":"DEFAULT:1","quantity":${quantity},"order":{"id":${orderId}}}}}""".stripMargin))
        .check(status.is(200))
      )
  }
}
