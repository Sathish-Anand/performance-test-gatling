package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker

import util.{Configurations, Environment, SetupParameters => Parameters}

import com.github.javafaker.Faker

object User extends Simulation {

  val faker = new Faker();
  val userPrefix = Parameters.stringPrefix

  val feeder = Iterator.continually(Map(
      "ref" -> userPrefix.concat("-").concat(faker.name().username()),
      "title" -> faker.name().title(),
      "firstName" -> faker.name().firstName(),
      "lastName" -> faker.name().lastName(),
      "password" -> faker.internet().password(),
      "email" -> faker.internet().emailAddress(),
      "status" -> faker.lorem().word(),
      "timezone" -> faker.address().timeZone(),
    ))


  val CreateUser = {
    feed(feeder)
      .exec(http("Create User")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateUserInput!) {\n  createUser(input: $input) {\n    id\n    ref\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","username":"${ref}","password":"${password}","firstName":"${firstName}","primaryEmail":"${email}","type":"LOCATION","timezone":"${timezone}"}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createUser.id").saveAs("userId"))
        .check(jsonPath("$.data.createUser.ref").saveAs("userRef"))
      )
  }

  val UpdateUser = {
    feed(feeder)
      .exec(http("Update User")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateUserInput!) {\n  updateUser(input: $input) {\n    id\n    ref\n    status\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${userId},"status":"${status}"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetUser = {
    feed(Carrier.feeder())
      .exec(http("Get Users")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  users(first:${feeders.pagination}){\n    edges{\n      node{  id\n        ref\n        status\n        type\n        country\n        createdOn\n        updatedOn\n        username\n        attributes{\n          name\n          type\n          value\n        }\n        primaryEmail\n        primaryPhone\n        department\n        firstName\n        lastName\n        primaryRetailer{id}\n        primaryLocation{id ref}\n        promotionOptIn\n\n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.users.edges[${feeders.getPagination}].cursor").saveAs("userFirstCursor"))
        .check(jsonPath("$.data.users.edges[${feeders.getPagination}].node.id").saveAs("userId"))
      )
  }

  val GetUserByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Users By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  users(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${userFirstCursor}\" status:\"CREATED\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{  id\n        ref\n        status\n        type\n        country\n        createdOn\n        updatedOn\n        username\n        attributes{\n          name\n          type\n          value\n        }\n        primaryEmail\n        primaryPhone\n        department\n        firstName\n        lastName\n        primaryRetailer{id}\n        primaryLocation{id ref}\n        promotionOptIn\n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetUserById = {
    exec(http("Get User By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query userById(\n$id:ID!\n){\n  userById(id:$id){  id\n        ref\n        status\n        type\n        country\n        createdOn\n        updatedOn\n        username\n        attributes{\n          name\n          type\n          value\n        }\n        primaryEmail\n        primaryPhone\n        department\n        firstName\n        lastName\n        primaryRetailer{id}\n        primaryLocation{id ref}\n        promotionOptIn\n }}\n",
          |"variables":{"id":"${userId}"},"operationName":"userById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val UserQueries = scenario("User Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetUser, GetUserByPagination, GetUserById)
    }
}