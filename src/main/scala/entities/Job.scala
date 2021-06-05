package entities

import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import java.io.FileWriter
import com.github.javafaker.Faker

object Job extends Simulation {

  var faker = new Faker();
  var jobId = ""

  def get(): Map[String, Any] = {
    Map(
      "name" -> faker.commerce().productName(),
      "retailerId" -> 0
    )
  }

  def feeder(): Iterator[Map[String, Any]] ={
    Iterator.continually(Map("job"-> get()))
  }

  val PostTestJob = {
    feed(feeder())
    .exec(http("Job Creation")
      .post("/api/v4.1/job")
      .headers(header.accountHeader)
      .body(StringBody("""
                         |{
                         |"name":"${job.name}",
                         |"retailerId": "${job.retailerId}"
                         |}
                         |""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.id").exists)
      .check(jsonPath("$.id").saveAs("jobId"))
    )
      .exec(session => {
        jobId = session("jobId").as[String]
        println("Current Job Id is " + jobId)
        session
      }
      )
  }

  val PostAccountJob = {
    feed(feeder())
      .exec(http("Job Account Creation")
        .post("/api/v4.1/job")
        .headers(header.superHeader)
        .body(StringBody("""
                           |{
                           |"name":"${job.name}",
                           |"retailerId": "${job.retailerId}"
                           |}
                           |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.id").exists)
        .check(jsonPath("$.id").saveAs("jobId"))
      )
      .exec(session => {
        jobId = session("jobId").as[String]
        println("Current Job Id is " + jobId)
        session
      }
      )
  }


  val PostRetailerJob = {
    feed(feeder())
      .exec(http("Job Retailer Creation")
        .post("/api/v4.1/job")
        .headers(header.accountHeader)
        .body(StringBody("""
                           |{
                           |"name":"${job.name}",
                           |"retailerId": "${job.retailerId}"
                           |}
                           |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.id").exists)
        .check(jsonPath("$.id").saveAs("jobId"))
      )
      .exec(session => {
        jobId = session("jobId").as[String]
        println("Current Job Id is " + jobId)
        session
      }
      )
  }

  val PostJob = {
    feed(feeder())
      .exec(http("Job Creation")
        .post("/api/v4.1/job")
        .headers(header.fluentHeader)
        .body(StringBody("""
                           |{
                           |"name":"${job.name}",
                           |"retailerId": "${retId}"
                           |}
                           |""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.id").exists)
        .check(jsonPath("$.id").saveAs("jobId"))
      )
      .exec(session => {
        jobId = session("jobId").as[String]
        println("Current Job Id is " + jobId)
        session
      }
      )
  }

  val getJob =
    exec(http("Get Created Job")
      .get("/api/v4.1/job/${jobId}")
      .headers(header.fluentHeader)
      .check(status.is(200))
      .check(jsonPath("$.jobId").exists)
      .check(jsonPath("$.status").saveAs("jobStatus"))
    )


}
