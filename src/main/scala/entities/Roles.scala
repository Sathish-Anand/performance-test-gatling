package entities

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

object Roles extends Simulation {

  val GetRole = {
    feed(Carrier.feeder())
      .exec(http("Get Roles")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  roles(first:${feeders.pagination}){\n    edges{\n      node{\n permissions{\n          name\n        }\n          id\n        name\n      }\n      cursor\n    }\n  }\n}"
            |}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.roles.edges[${feeders.getPagination}].cursor").saveAs("roleFirstCursor"))
        .check(jsonPath("$.data.roles.edges[${feeders.getPagination}].node.name").saveAs("roleName"))
      )
  }

  val GetRoleByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Roles By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query {\n  roles(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${roleFirstCursor}\"){\n    edges{\n      node{\n permissions{\n          name\n        }\n          id\n        name\n     }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetRoleById = {
    exec(http("Get Role By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"query role(\n$name:String!\n){\n  role(name:$name){permissions{\n          name\n        }\n          id\n        name\n     }}\n",
          |"variables":{"name":"${roleName}"},"operationName":"role"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..name").exists)
    )
  }

  val RoleQueries = scenario("Role Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetRole, GetRoleByPagination, GetRoleById)
    }
}
