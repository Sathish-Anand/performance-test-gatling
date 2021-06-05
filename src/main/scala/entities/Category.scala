package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import com.github.javafaker.Faker


object Category extends Simulation {

  var faker = new Faker()

  var users = Configurations.users
  var categoryCount = new AtomicInteger(1)
  var categoryPrefix = Parameters.stringPrefix
  var categories = Parameters.category/users
//  var totalCategory = Math.round(categories)
  var totalCategory = Parameters.category
  var category = categoryPrefix+"_Cat_"

  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "ref" -> category,
      "number" -> categoryCount.getAndIncrement(),
      "type" -> "DEFAULT"
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("category"-> get()))
  }

  val PostCategory =
  repeat(totalCategory){
    feed(feeder())
    .exec(http("Create Category")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"mutation{\n  createCategory(input:{\n    ref:\"${category.ref}${retId}${category.number}\"\n    catalogue:{\n      ref:\"DEFAULT:${retId}\"\n    }\n    type:\"${category.type}\"\n    name:\"${category.name}\"\n  }){\n    ref\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").saveAs("catRef"))
    )
      .exitHereIfFailed
  }

  val CategorySetup = scenario("CATEGORIES SETUP")
  .exec(Category.PostCategory)
     .exec(session => {
          categoryCount = new AtomicInteger(1)
          println("Current Value is " + categoryCount)
          session
     })

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("Categories"-> Carrier.get()))
  }

  val GetCategory = {
    feed(queryFeeder())
      .exec(http("Get Categories")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"{\n  categories(first:${Categories.pagination}){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n  catalogue{\n          ref\n        }\n      }\n      cursor\n    }\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.categories.edges[${Categories.getPagination}].cursor").saveAs("catFirstCursor"))
      .check(jsonPath("$.data.categories.edges[${Categories.getPagination}].node.ref").saveAs("catRef"))
      .check(jsonPath("$.data.categories.edges[${Categories.getPagination}].node.catalogue.ref").saveAs("prodCatRef"))
    )
  }

  val GetCategoryByPagination = {
    feed(queryFeeder())
      .exec(http("Get category By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query category($first:Int,\n  $after:String,\n  $from:DateTime,\n  $to:DateTime \n){\n  categories(first:$first after:$after status:\"CREATED\" createdOn:{from:$from,to:$to} ){\n    edges{\n      node{\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n  catalogue{\n          ref\n        }\n      }\n      cursor\n    }\n  }\n}",
            |"variables":{"${Categories.firstOrLast}":${Categories.pagination}, "${Categories.afterOrBefore}":"${catFirstCursor}","from":"${Categories.from}","to":"${Categories.to}"},
            |"operationName":"category"}
            |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetCategoryById = {
    exec(http("Get category By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query category(\n$ref:String!\n  $prodCatRef:String!\n){\n  category(ref:$ref catalogue:{ref:$prodCatRef}){\n        id\n        ref\n        type\n        workflowRef\n        workflowVersion\n        status\n        createdOn\n        updatedOn \n      }}\n","variables":{"ref":"${catRef}","prodCatRef":"${prodCatRef}"},"operationName":"category"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }

  val CategoryQueries = scenario("Categories Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(
        // GetCategory, GetCategoryByPagination,
        GetCategoryById)
    }

}
