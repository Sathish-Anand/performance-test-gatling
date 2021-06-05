package dataSetup

import util.{Configurations, Environment, SetupParameters => Parameters}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.http.Predef.{http, status, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import entities._
import io.gatling.core.Predef._

/*** mvn clean install
 * -Dgatling.simulationClass=dataSetup.dataSetup.GraphQLQueriesTest
 * -DfromUser=1 -DtoUser=1 -Dramp_up_duration=2
 * -DbaseURL=http://test.api.fluentretail.com -DstartWith=SATHISH
 */

class productAndInventory extends Simulation {

  val accountCount = new AtomicInteger(1)
  val retailerCount = new AtomicInteger(1)
  val networkCount = new AtomicInteger(1)
  val noOfAccount = Parameters.account
  val noOfNetwork = Parameters.network
  val noOfRetailer = Parameters.retailer


  val httpConfiguration = http.baseUrl(Environment.baseURL).disableCaching

  val RetailerLogin = scenario("Retailer Login")
    .exec(Oauth.JsonRetailerAuth)
//      .inject(atOnceUsers(1))

  val RetailerLogins = scenario("Retailer Login")
    .exec(Oauth.JsonRetailerAuth_1)

  val RetailerLogin2 = scenario("Retailer Login")
    .exec(Oauth.JsonRetailerAuth_2)

  val NetworkLocationSetup = scenario("Network and Location Setup")
    .repeat(noOfNetwork) {
      exec(Network.PostNetwork, Location.PostLocation)
    }

  val ProductSetup = scenario("PRODUCTS SETUP")
    .exec(Product.standardVariantProductSetup)
  //    .inject(atOnceUsers(10))

  val VirtualPositionsSetup = scenario("Virtual Postions Setup")
    .exec(VirtualPosition.virtualPositionChildProducts, VirtualPosition.virtualPositionStdParentProducts, VirtualPosition.virtualPositionVarParentProducts)
  //    .inject(atOnceUsers(10))

  val InventoryPositionsSetup = scenario("Inventory Postions Setup")
    .exec(InventoryPosition.InventoryPositionStandardVariantProducts, InventoryPosition.InventoryPositionStandardOnlyProducts, InventoryPosition.InventoryPositionVariantOnlyProducts)
  //    .inject(atOnceUsers(10))

  val InventoryQuantitesSetup = scenario("Inventory Qiantities Setup")
    .exec(InventoryQuantity.InventoryQuantityForAll)
  //    .inject(atOnceUsers(10))

//  val RetailerCreationAndSetup = scenario("Retailer Creation And Data Setup")
//    .asLongAs(_ => retailerCount.getAndIncrement() <= noOfRetailer) {
//      exec(RetailerCreation, RetailerSetup)
//      exec(VirtualPositionsSetup, InventoryPositionsSetup)
//    }
//    .inject(atOnceUsers(1))

  val RetailerNetworkLocationSetup = scenario("Network and Location Setup")
    .repeat(noOfRetailer, "counters") {
      exec(RetailerLogins, NetworkLocationSetup)
    }
    .exec(session => {
      var counter = 0
      session
    })

  val ProductVirtualSetup = scenario("Category, Standard & Variant Product, Virtual Position Setup")
    .repeat(noOfRetailer, "counter") {
      exec(RetailerLogin, Category.CategorySetup, ProductSetup, VirtualPositionsSetup)
    }
    .exec(session => {
      var counter = 0
      session
    })

  val InventorySetup = scenario("Inventory Position and Quantities Setup")
    .repeat(noOfRetailer, "counter2") {
      exec(RetailerLogin2, InventoryPositionsSetup)
    }
    .exec(session => {
      var counter2 = 0
      session
    })

  setUp(
    RetailerNetworkLocationSetup.inject(atOnceUsers(1)),
    ProductVirtualSetup.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    InventorySetup.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds))
  ).protocols(httpConfiguration)

//  setUp(RetailerLogin.inject(atOnceUsers(1))
//    .protocols(httpConfiguration)
//  setUp(retailerSetup(2).inject(atOnceUsers(1)).protocols(httpConfiguration))

    .assertions(
      global.successfulRequests.percent.gt(95),
      forAll.failedRequests.percent.lt(20)
        // Assert that every request has no more than 20% of failing requests
    )
 }

