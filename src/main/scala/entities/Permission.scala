package entities

import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import com.github.javafaker.Faker


object Permission extends Simulation {

  var faker = new Faker();

  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "accountId" -> 0
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("permissions"-> get()))
  }

  val PutAccountPermissions = {
    feed(feeder())
    .exec(http("Account Permissions")
      .put("/api/v4.1/user/${accName}_admin/grant")
      .headers(header.accountHeader)
      .body(StringBody("""{
                         |    "roleContextList":[
                         |    {
                         |      "roleId": "DEVELOPER",
                         |      "contextType": "ACCOUNT",
                         |      "contextId": "${permissions.accountId}"
                         |    },
                         |
                         |    {
                         |      "roleId": "ADMIN",
                         |      "contextType": "ACCOUNT",
                         |      "contextId": "${permissions.accountId}"
                         |    },
                         |    {
                         |      "roleId": "GRAPHQL",
                         |      "contextType": "ACCOUNT",
                         |      "contextId": "${permissions.accountId}"
                         |    },
                         |		{
                         |      "roleId": "ROLE_MANAGER",
                         |      "contextType": "ACCOUNT",
                         |      "contextId": "${permissions.accountId}"
                         |    },
                         |		{
                         |      "roleId": "USER_MANAGER",
                         |      "contextType": "ACCOUNT",
                         |      "contextId": "${permissions.accountId}"
                         |    }
                         |  ]
                         |}""".stripMargin))
      .check(status.is(200))
    )
    }

  val PutRetailerPermissions = {
    feed(feeder())
      .exec(http("Retailer Permissions")
//        .put("/api/v4.1/user/${userAuth.retUsername}/grant")
        .put("/api/v4.1/user/${retUsername}/grant")
        .headers(header.accountHeader)
        .body(ElFileBody("data/dataSetup/retailerPermissions.json"))
        .check(status.is(200))
      )
  }

  val PutRetailerRubixPermissions = {
    feed(feeder())
      .exec(http("Retailer Rubix Permissions")
//        .put("/api/v4.1/user/${userAuth.clientId}-${retId}-rubix/grant")
        .put("/api/v4.1/user/${clientId}-${retId}-rubix/grant")
        .headers(header.accountHeader)
        .body(ElFileBody("data/dataSetup/retailerPermissions.json"))
        .check(status.is(200))
      )
  }
}
