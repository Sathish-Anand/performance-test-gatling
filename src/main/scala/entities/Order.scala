package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import java.io.FileWriter
import com.github.javafaker.Faker
import scala.util.Random
import java.util.Calendar
import java.time.OffsetDateTime
import java.time.Duration
import java.time.Clock

object Order extends Simulation {

  var faker = new Faker();
  val rnd = new Random()
  var orderPrefix = Parameters.stringPrefix
  var customerPrefix = orderPrefix+"CUSTOMER"
  var orderCount = new AtomicInteger(1)
  var expectedOrderStatus = "BOOKED"
  var orderStatus = ""
  var orderRef = ""
  var createdOnTime = ""
  var updatedOnTime = ""

  val orderFeeder = Iterator.continually(Map(
      "orderRef" -> orderPrefix,
      "number" -> orderCount.getAndIncrement(),
      "random" -> rnd.nextInt(1000),
      "customerRef" -> customerPrefix,
      "firstName" -> faker.name().firstName(),
      "lastName" -> faker.name().lastName(),
      "status" -> "booked",
      "email" -> faker.internet().emailAddress()
    ))

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("orders"-> Carrier.get()))
  }

  val PostHDOrder = {
    feed(orderFeeder)
    .exec(http("HD Order Creation")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(ElFileBody("data/dataSetup/orderHD.json"))
      .check(status.is(200))
      .check(jsonPath("$.data.HDO.id").saveAs("orderId"))
      .check(jsonPath("$.data.HDO.ref").saveAs("orderRef"))
      .check(jsonPath("$.data.HDO.status").saveAs("orderStatus"))
    )
      .exec(session => {
        orderStatus = session("orderStatus").as[String]
        println("Current HD Order Status is " + orderStatus)
        session
          }
      )
  }

  val GetOrderById =
    doWhile(_ => !expectedOrderStatus.equals(orderStatus), "OrderStatusCheck") {
      exec(http("Get Created Order")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(ElFileBody("data/dataSetup/getOrderById.json"))
        .check(status.is(200))
        .check(jsonPath("$.data.order.status").saveAs("orderStatus"))
        .check(jsonPath("$.data.order.ref").saveAs("orderRef"))
        .check(jsonPath("$.data.order.createdOn").saveAs("createdOnTime"))
        .check(jsonPath("$.data.order.updatedOn").saveAs("updatedOnTime"))
      )
        .exec(session => {
          orderStatus = session("orderStatus").as[String]
          println("Current HD Order Status is " + orderStatus)
          session
        }
        )
        .exitHereIfFailed
    }

  val GetOrderByIdAfterStatusChange =
    {
      exec(http("Get Created Order After Status Complete")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(ElFileBody("data/dataSetup/getOrderById.json"))
        .check(status.is(200))
        .check(jsonPath("$.data.order.status").saveAs("orderStatus"))
        .check(jsonPath("$.data.order.ref").saveAs("orderRef"))
        .check(jsonPath("$.data.order.createdOn").saveAs("createdOnTime"))
        .check(jsonPath("$.data.order.updatedOn").saveAs("updatedOnTime"))
      )
        .exec(session => {
          orderStatus = session("orderStatus").as[String]
          orderRef = session("orderRef").as[String]
          createdOnTime = session("createdOnTime").as[String]
          updatedOnTime = session("updatedOnTime").as[String]
          println("Current HD Order Status is " + orderStatus)
          if (session("orderStatus").as[String].equals(expectedOrderStatus)) {
            val fw = new FileWriter("orderUpdatedTime.csv", true)
            val orderCreatedTime = OffsetDateTime.parse(createdOnTime)
            val orderUpdatedTime = OffsetDateTime.parse(updatedOnTime)
            val duration = Duration.between(orderCreatedTime, orderUpdatedTime)
            try {
              fw.write( session("orderRef").as[String] + ", "
                + orderCreatedTime + ", " + orderUpdatedTime + ", "+ "Total_time_to_update_delta_in_milli_seconds, " + duration.toMillis + "\n")
              println("Total Time Taken To Update Order Status: " + duration)
              println("Onhand matches")
            }
            finally fw.close()
          }
          else {
            println("Current HD Order Status is " + orderStatus)
          }
          session
        }
        )
        .exitHereIfFailed
    }


  val GetOrder = {
    feed(queryFeeder())
      .exec(http("Get Orders")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """|{
          |"query":"query {\n  orders(first:${orders.pagination}){\n    edges{\n      node{\n        id\n        ref\n        status\n    createdOn\n    updatedOn\n        fulfilmentChoice{\n            pickupLocationRef\n            deliveryType\n            fulfilmentType\n            fulfilmentPrice\n            fulfilmentTaxPrice\n            currency\n            deliveryAddress{\n                name\n                ref\n                latitude\n                longitude\n            }\n        }\n        customer{\n            id\n            username\n            country\n            lastName\n            firstName\n            primaryEmail\n            primaryPhone\n            retailer{\n                id\n            }\n        }\n        items{\n            edges{\n                node{\n                    ref\n\n                    quantity\n                }\n            }\n        }\n      }cursor\n    }}\n}"
          |}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.orders.edges[${orders.getPagination}].cursor").saveAs("ordFirstCursor"))
      .check(jsonPath("$.data.orders.edges[${orders.getPagination}].node.ref").saveAs("ordRef"))
    )
  }

  val GetOrderByPagination = {
    feed(queryFeeder())
      .exec(http("Get Order By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  orders(${orders.firstOrLast}:${orders.pagination} ${orders.afterOrBefore}:\"${ordFirstCursor}\" status:\"BOOKED\" createdOn:{from:\"${orders.from}\",to:\"${orders.to}\"} ){\n    edges{\n      node{\n         id\n        ref\n        status\n    createdOn\n    updatedOn\n        fulfilmentChoice{\n            pickupLocationRef\n            deliveryType\n            fulfilmentType\n            fulfilmentPrice\n            fulfilmentTaxPrice\n            currency\n            deliveryAddress{\n                name\n                ref\n                latitude\n                longitude\n            }\n        }\n        customer{\n            id\n            username\n            country\n            lastName\n            firstName\n            primaryEmail\n            primaryPhone\n            retailer{\n                id\n            }\n        }\n        items{\n            edges{\n                node{\n                    ref\n\n                    quantity\n      }\n     }\n  }\n}}}}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetOrderByRef = {
    exec(http("Get Order By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query order(\n$ref:String!\n){\n  order(ref:$ref){\n     id\n        ref\n        status\n    createdOn\n    updatedOn\n        fulfilmentChoice{\n            pickupLocationRef\n            deliveryType\n            fulfilmentType\n            fulfilmentPrice\n            fulfilmentTaxPrice\n            currency\n            deliveryAddress{\n                name\n                ref\n                latitude\n                longitude\n            }\n        }\n        customer{\n            id\n            username\n            country\n            lastName\n            firstName\n            primaryEmail\n            primaryPhone\n            retailer{\n                id\n            }\n        }\n        items{\n            edges{\n                node{\n                    ref\n\n                    quantity\n   }}\n}}}",
          |"variables":{"ref":"${ordRef}"},"operationName":"order"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val OrderQueries = scenario("Orders Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetOrder, GetOrderByPagination, GetOrderByRef)
    }

}
