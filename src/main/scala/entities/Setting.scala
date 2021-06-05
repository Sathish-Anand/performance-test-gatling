package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker


object Setting extends Simulation {

  var faker = new Faker();

  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "accountId" -> 0
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("setting"-> get()))
  }

  val PostAccountSetting = {
    feed(feeder())
    .exec(http("Account Settings")
      .post("/graphql")
      .headers(header.accountHeader)
      .body(ElFileBody("data/dataSetup/accountSettings.json"))
      .check(status.is(200))
    )
  }

  val PostRetailerSetting = {
    feed(feeder())
      .exec(http("Retailer Settings")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(ElFileBody("data/dataSetup/retailerSettings.json"))
        .check(status.is(200))
      )
  }

  val GetSetting = {
    feed(Carrier.feeder())
      .exec(http("Get Settings")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"{\n  settings(first:${feeders.pagination}){\n    edges{\n      node{\n        id\n        name\n        context\n        contextId\n        lobValue\n        value\n        valueType\n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.settings.edges[${feeders.getPagination}].cursor").saveAs("setFirstCursor"))
        .check(jsonPath("$.data.settings.edges[${feeders.getPagination}].node.id").saveAs("settingId"))
      )
  }

  val GetSettingByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Setting By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  settings(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${setFirstCursor}\" ){\n    edges{\n      node{\n        id\n        name\n        context\n        contextId\n        lobValue\n        value\n        valueType\n       }\n      cursor\n    }\n  }\n}"}
           |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetSettingById = {
    exec(http("Get Setting By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query setting(\n$id:ID!\n){\n  setting(id:$id)\n  {\n          id\n        name\n        context\n        contextId\n        lobValue\n        value\n        valueType\n     }\n}\n",
          |"variables":{"id":"${settingId}"},"operationName":"setting"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val SettingQueries = scenario("Setting Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetSetting, GetSettingByPagination, GetSettingById)
    }



}
