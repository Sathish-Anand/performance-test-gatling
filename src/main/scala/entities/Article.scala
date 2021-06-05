package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._
import com.github.javafaker.Faker
import scala.util.Random

object Article extends Simulation {

  var faker = new Faker();
  val rnd = new Random()
  var articlePrefix = Parameters.stringPrefix

  val articleFeeder = Iterator.continually(Map(
    "ref" -> articlePrefix.concat("-").concat(rnd.nextInt(1000000).toString),
    "type" -> faker.commerce().productName(),
    "width" -> faker.number().numberBetween(10, 100),
    "height" -> faker.number().randomDouble(3, 10, 30),
    "length" -> faker.number().randomDouble(3, 10, 30),
    "weight" -> faker.number().randomDouble(3, 10, 30)
  ))

  val CreateArticle = {
    feed(articleFeeder)
      .exec(http("Create Article")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateArticleInput!) {\n  createArticle(input: $input) {\n    id\n    ref\n    type\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"ref":"${ref}","type":"${type}","height":${height},"length":${length},"width":${width},"weight":${weight},"fulfilments":[{"id":${fulfilmentId}}]}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createArticle.id").saveAs("articleId"))
      )
  }

  val UpdateArticle = {
    feed(articleFeeder)
      .exec(http("Update Article")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateArticleInput!) {\n  updateArticle(input: $input) {\n    id\n    ref\n    type\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${articleId},"height":${height}}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetArticle = {
    feed(Carrier.feeder())
      .exec(http("Get Articles")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"{\n  articles(first:${feeders.pagination}){\n    edges{\n      node{ id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        createdOn\n        updatedOn\n        description\n        length\n        height\n        width\n        weight\n        fulfilments{edges{node{ref}}}\n        consignmentArticles{edges{node{article{ref}}}}\n        carrierConsignmentArticles{edges{node{article{id}}}}\n        items{edges{node{id}}}\n        quantity\n      }\n      cursor\n    }\n  }\n}"}
            |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.articles.edges[${feeders.getPagination}].cursor").saveAs("artFirstCursor"))
        .check(jsonPath("$.data.articles.edges[${feeders.getPagination}].node.id").saveAs("artId"))
      )
  }

  val GetArticleByPagination = {
    feed(Carrier.feeder())
      .exec(http("Get Articles By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """|{"query":"query {\n  articles(${feeders.firstOrLast}:${feeders.pagination} ${feeders.afterOrBefore}:\"${artFirstCursor}\" createdOn:{from:\"${feeders.from}\",to:\"${feeders.to}\"} ){\n    edges{\n      node{\n  id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        createdOn\n        updatedOn\n        description\n        length\n        height\n        width\n        weight\n        fulfilments{edges{node{ref}}}\n        consignmentArticles{edges{node{article{ref}}}}\n        carrierConsignmentArticles{edges{node{article{id}}}}\n        items{edges{node{id}}}\n        quantity\n   }\n      cursor\n    }\n  }\n}"}
             |""".stripMargin))
        .check(status.is(200))
      )
  }

  val GetArticleById = {
    exec(http("Get Articles By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query articleById(\n$id:ID!\n){\n  articleById(id:$id)\n  { id\n        ref\n        status\n        workflowRef\n        workflowVersion\n        type\n        createdOn\n        updatedOn\n        description\n        length\n        height\n        width\n        weight\n        fulfilments{edges{node{ref}}}\n        consignmentArticles{edges{node{article{ref}}}}\n        carrierConsignmentArticles{edges{node{article{id}}}}\n        items{edges{node{id}}}\n        quantity\n      }\n}\n",
          |"variables":{"id":"${artId}"},
          |"operationName":"articleById"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..ref").exists)
    )
  }
  val ArticleQueries = scenario("Articles Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetArticle, GetArticleByPagination, GetArticleById)
    }
}
