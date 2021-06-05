package entities

import java.util.Random

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef._

object Comment extends Simulation {

  val rnd = new Random()
  val r = new scala.util.Random(31)

  val newCommentFeeder = Iterator.continually(Map(
    "id" -> rnd.nextInt(1000000000),
    "text" -> r.nextString(10)
  ))

  val updateCommentFeeder = Iterator.continually(Map(
    "updatedText" -> r.nextString(10)
  ))

  val CreateComment = {
    feed(newCommentFeeder)
    .exec(http("Create Comment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: CreateCommentInput!) {\n  createComment(input: $input) {\n    id\n text\n  entityId\n    entityType\n    createdOn\n  }\n}","variables":{"input":{"entityType":"ORDER","entityId":${id},"text":"${text}"}}}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.createComment.id").saveAs("commentId"))
      )
  }

  val UpdateComment = {
    feed(updateCommentFeeder)
      .exec(http("Update Comment")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """{"query":"mutation($input: UpdateCommentInput!) {\n  updateComment(input: $input) {\n    id\n    text\n   entityId\n    createdOn\n    updatedOn\n  }\n}","variables":{"input":{"id":${commentId},"text":"${updatedText}"}}}""".stripMargin))
        .check(status.is(200))
      )
  }

  def queryFeeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("Comments"-> Carrier.get()))
  }

  val GetComment = {
    feed(queryFeeder())
    .exec(http("Get Comments")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{
          |"query":"{\n  comments(first:${Comments.pagination}){\n    edges{\n      node{\n        id\n       text\n        entityId\n        entityType\n         createdOn\n        updatedOn \n      }\n      cursor\n    }\n  }\n}"
          |}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data.comments.edges[${Comments.getPagination}].cursor").saveAs("comFirstCursor"))
      .check(jsonPath("$.data.comments.edges[${Comments.getPagination}].node.id").saveAs("commentId"))
    )
  }

  val GetCommentByPagination = {
    feed(queryFeeder())
      .exec(http("Get Comments By Pagination")
        .post("/graphql")
        .headers(header.fluentHeader)
        .body(StringBody(
          """
            |{
            |"query":"query comment($first:Int,\n  $after:String,\n  $from:DateTime,\n  $to:DateTime \n){\n  comments(first:$first after:$after createdOn:{from:$from,to:$to} ){\n    edges{\n      node{\n        id\n        text\n        entityId\n        entityType\n    createdOn\n        updatedOn \n     }\n      cursor\n    }\n  }\n}",
            |"variables":{"${Comments.firstOrLast}":${Comments.pagination}, "${Comments.afterOrBefore}":"${comFirstCursor}","from":"${Comments.from}","to":"${Comments.to}"},
            |"operationName":"comment"}
            |""".stripMargin))
        .check(status.is(200))

      )
  }

  val GetCommentById = {
    exec(http("Get Comment By ID")
      .post("/graphql")
      .headers(header.fluentHeader)
      .body(StringBody(
        """
          |{"query":"query comment(\n$id:ID!\n){\n  commentById(id:$id){\n        id\n          text\n        entityId\n        entityType\n    createdOn\n        updatedOn \n      }}\n",
          |"variables":{"id":"${commentId}"},
          |"operationName":"comment"}
          |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.data..id").exists)
    )
  }

  val CommentQueries = scenario("Comments Query search, byId, with paginations")
    .repeat(Carrier.noOfRepeat) {
      exec(GetComment, GetCommentByPagination, GetCommentById)
    }
}
