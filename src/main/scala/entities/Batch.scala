package entities

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import java.io.FileWriter
import com.github.javafaker.Faker

object Batch extends Simulation {

  var faker = new Faker();
  //  var accountprefix = Parameters.stringPrefix;
  val retailerCounts = new AtomicInteger(1)
  var accountprefix = Parameters.stringPrefix
  var accountNameTest = accountprefix
  val noOfRetailer = Parameters.retailer
  var expectedBatchStatus = "COMPLETE"
  var clientSecret = ""
  var accUsername = ""
  var accPassword  = ""
  var retUsername = ""
  var retPassword  = ""
  var batchStatus = ""
  var accountRef = ""
  var batchId = ""
  var entityId = ""
  var scope = ""

  def get(): Map[String, Any] = {
      Map(
      "action" -> "CREATE",
      "entityType" -> "ACCOUNT",
      "accountId" -> accountNameTest,
      "accountName" -> accountNameTest,
      "retailerId" -> faker.name().firstName(),
      "retailerName" -> faker.name().firstName(),
      "region" -> "AU",
      "tradingName" -> faker.commerce().productName(),
      "customerSupportPhone" -> faker.phoneNumber().cellPhone(),
      "customerSupportEmail" -> faker.internet().emailAddress(),
      "website" -> faker.company().url()
    )
  }

  def feeder(): Iterator[Map[String, Any]] = {
    Iterator.continually(Map("batch" -> get()))
  }

  val PostAccountBatch = {
    feed(feeder())
    .exec(http("Account Batch Creation")
      .post("/api/v4.1/job/${jobId}/batch")
      .headers(header.superHeader)
      .body(StringBody(
        """
          |{
          |  "action": "${batch.action}",
          |  "entityType": "${batch.entityType}",
          |  "entities": [
          |    {
          |      "accountId": "${batch.accountId}${jobId}",
          |      "accountName": "${batch.accountName}${jobId}",
          |      "database": "postgres01"
          |    }
          |  ]
          |}
          |""".stripMargin)).asJson
      .check(status.is(200))
      .check(jsonPath("$.id").exists)
      .check(jsonPath("$.id").saveAs("batchId"))
    )
  }

  val PostRetailerBatch = {
    feed(feeder())
      .exec(http("Retailer Batch Creation")
        .post("/api/v4.1/job/${jobId}/batch")
        .headers(header.accountHeader)
        .body(StringBody(
          """|{
            |  "action": "${batch.action}",
            |  "entityType": "RETAILER",
            |  "entities": [
            |    {
            |      "retailerId":"${batch.retailerId}",
            |      "retailerName":"${batch.retailerName}",
            |      "region":"${batch.region}",
            |      "tradingName":"${batch.tradingName}",
            |      "customerSupportPhone":"${batch.customerSupportPhone}",
            |      "customerSupportEmail":"${batch.customerSupportEmail}",
            |      "website":"${batch.website}"
            |    }
            |  ]
            |}""".stripMargin)).asJson
        .check(status.is(200))
        .check(jsonPath("$.id").exists)
        .check(jsonPath("$.id").saveAs("retBatchId"))
      )
  }

  val GetAccountBatch =
    doWhile(_ => !expectedBatchStatus.equals(batchStatus), "StatusCheck") {
      exec(http("Get Account Created Batch")
        .get("/api/v4.1/job/${jobId}/batch/${batchId}")
        .headers(header.superHeader)
        .check(status.is(200))
        .check(jsonPath("$.batchId").exists)
        .check(jsonPath("$.status").saveAs("batchStatus"))
      )
        .exec(session => {
          batchStatus = session("batchStatus").as[String]
          println("Current Batch Status is " + batchStatus)
          session
        }
        )
      .exitHereIfFailed
    }

  val GetAccountBatchAfterComplete =
    exec(http("Get Created Account Batch After It Completes")
      .get("/api/v4.1/job/${jobId}/batch/${batchId}")
      .headers(header.superHeader)
      .check(status.is(200))
      .check(jsonPath("$.batchId").exists)
      .check(jsonPath("$.status").saveAs("batchStatus"))
      .check(jsonPath("$.results[0].entityId").saveAs("clientId"))
      .check(jsonPath("$.results[0].entityRef").saveAs("clientIdLowerCase"))
      .check(jsonPath("$.results[0].clientSecret").saveAs("clientSecret"))
      .check(jsonPath("$.results[0].username").saveAs("accUsername"))
      .check(jsonPath("$.results[0].password").saveAs("accPassword"))
    )
      .exec(session => {
        batchStatus = session("batchStatus").as[String]
        accountRef = session("clientId").as[String]
        clientSecret = session("clientSecret").as[String]
        accUsername = session("accUsername").as[String]
        accPassword = session("accPassword").as[String]
        println("Current Batch Status for a " + accountRef + " is " + batchStatus)
        if (session("batchStatus").as[String].equals(expectedBatchStatus)) {
          val fw = new FileWriter(accountRef + ".json", true)
          try {
            fw.write("[{"+ "\n" +
              "\"clientId\":" + "\"" + accountRef + "\"," + "\n" +
              "\"clientSecret\":" + "\"" + clientSecret + "\"," + "\n" +
              "\"accPassword\":" + "\"" + accPassword + "\"," + "\n" +
              "\"accUsername\":" + "\"" + accUsername + "\"," + "\n")
            println("Account Creation for " + accountRef + " is successful")
          }
          finally fw.close()
        }
        else {
          println("Status Match Failed")
        }
        session
      }
      )

  val GetRetailerBatch =
    doWhile(_ => !expectedBatchStatus.equals(batchStatus), "StatusCheck") {
      exec(http("Get Created Retailer Batch")
        .get("/api/v4.1/job/${jobId}/batch/${retBatchId}")
        .headers(header.accountHeader)
        .check(status.is(200))
        .check(jsonPath("$.batchId").exists)
        .check(jsonPath("$.status").saveAs("batchStatus"))
      )
        .exec(session => {
          batchStatus = session("batchStatus").as[String]
          println("Current Batch Status is " + batchStatus)
          session
        }
        )
        .exitHereIfFailed
    }

  val GetRetailerBatchAfterComplete =
    exec(http("Get Created Retailer Batch After It Completes")
      .get("/api/v4.1/job/${jobId}/batch/${retBatchId}")
      .headers(header.accountHeader)
      .check(status.is(200))
      .check(jsonPath("$.batchId").exists)
      .check(jsonPath("$.status").saveAs("batchStatus"))
      .check(jsonPath("$.results[0].entityId").saveAs("entityId"))
      .check(jsonPath("$.results[0].username").saveAs("retUsername"))
      .check(jsonPath("$.results[0].password").saveAs("retPassword"))
    )
  .exec(session => {
    batchStatus = session("batchStatus").as[String]
    retUsername = session("retUsername").as[String]
    retPassword = session("retPassword").as[String]
    entityId = session("entityId").as[String]
    println("Current Batch Status for a " + accountRef + " is " + batchStatus)
    if (session("batchStatus").as[String].equals(expectedBatchStatus)) {

      val fw = new FileWriter(accountRef + ".json", true)
      val retCount = retailerCounts.getAndIncrement()
      if (retCount == noOfRetailer) {
        try {
          fw.write(
            "\"retPassword_" + entityId + "\"" + ":" + "\"" + retPassword + "\"," + "\n" +
              "\"retUsername_" + entityId + "\"" +  ":" + "\"" + retUsername + "\"" + "\n" +
              "}]")
        }
        finally fw.close()
      }
      else {
      try {
        fw.write(
          "\"retPassword_" + entityId + "\"" +  ":" + "\"" + retPassword + "\"," + "\n" +
            "\"retUsername_" + entityId + "\"" +  ":" + "\"" + retUsername + "\"," + "\n")
      }
      finally fw.close()
    }
  }
    else {
      println("Status Match Failed")
    }
    session
  }
  )
}
