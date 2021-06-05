package dataSetup

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import authentication.{SuperAuthentication => auth, testAuth =>authTest}
import io.gatling.core.Predef._
import entities._
import io.gatling.core.Predef._
import java.io.FileWriter
import scala.util.Random
import faker._
import io.gatling.core.structure.PopulationBuilder


/*** mvn clean install
 * -DfromUser=1 -DtoUser=1 -Dramp_up_duration=2
 * -DbaseURL=http://test.api.fluentretail.com -DstartWith=SATHISH
 */

class retailerSetup extends Simulation {

  val accountCount = new AtomicInteger(1)
  val retailerCount = new AtomicInteger(1)
  val networkCount = new AtomicInteger(1)
  val noOfAccount = Parameters.account
  val noOfNetwork = Parameters.network
  val noOfRetailer = Parameters.retailer

  var clientId = ""
  var accName = ""
  var accAuth = ""
  var accUsername = ""
  var retName = ""
  var retUsername = ""
  var retAuth = ""
  var retId = ""

  val httpConfiguration = http.baseUrl(Environment.baseURL).disableCaching

  val AccountCreation = scenario("New Account Creation")
    .exec(auth.SuperAuth, Job.PostAccountJob, Batch.PostAccountBatch, Batch.GetAccountBatch, Batch.GetAccountBatchAfterComplete, Oauth.AccountAuth, Event.PostEventSetup)


  val RetailerCreation = scenario("Account Settings & Permissions Setup and Retailer Creation")
    .exec(Permission.PutAccountPermissions, Setting.PostAccountSetting, Job.PostRetailerJob, Batch.PostRetailerBatch, Batch.GetRetailerBatch, Batch.GetRetailerBatchAfterComplete, Oauth.RetailerAuth)


  val RetailerSetup = scenario("Retailer Permissions, Settings and Carrier")
    .exec(Permission.PutRetailerPermissions, Permission.PutRetailerRubixPermissions, Setting.PostRetailerSetting, Carrier.PostCarrier, Workflow.PutInventoryWorkflow, Workflow.PutOrderWorkflow)


  val NetworkLocationSetup = scenario("Network and Location Setup")
    .repeat(noOfNetwork) {
      exec(Network.PostNetwork, Location.PostLocation)
    }

  val CatalogueSetup = scenario("Inventory Catalogue Setups")
    .exec(ProductCatalogue.PostProductCatalogue, InventoryCatalogue.PostInventoryCatalogue, VirtualCatalogue.PostVirtualCatalogue)


  val RetailerCreationAndSetup = scenario("Retailer Creation And Data Setup")
    .asLongAs(_ => retailerCount.getAndIncrement() <= noOfRetailer) {
      exec(RetailerCreation, RetailerSetup, NetworkLocationSetup, CatalogueSetup)
    }

  val dataSetup = scenario("Full Account Setup")
    .asLongAs(_ => accountCount.getAndIncrement() <= noOfAccount) {
      exec(AccountCreation, RetailerCreationAndSetup)
    }

  setUp(
    dataSetup.inject(
      atOnceUsers(Configurations.users)
    ).protocols(httpConfiguration)
  )
    .assertions(
      global.successfulRequests.percent.gt(95),
      // Assert that every request has no more than 10% of failing requests
      forAll.failedRequests.percent.lte(1)
    )
}