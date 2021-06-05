package dataSetup

import util.{Configurations, Environment, SetupParameters => Parameters}
import entities._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import util.Environment

import scala.concurrent.duration._

/*** mvn clean install
 * -Dgatling.simulationClass=dataSetup.dataSetup.GraphQLQueriesTest
 * -users=1 -rampUserDuration=2
 * -DbaseURL=http://test.api.fluentretail.com -Daccount=SATHISH
 */


class GraphQLQueriesTest extends Simulation {

  val httpConfiguration = http.baseUrl(Environment.baseURL).disableCaching

  val NetworksAndLocations = scenario("Networks And Locations And Carriers")
    .exec(Network.NetworkQueries,Location.LocationQueries, Carrier.CarrierQueries, StoreAddress.StoreAddressQueries)

  val Catalogues = scenario("Product, Inventory and Virtual Catalogues")
    .exec(ProductCatalogue.ProductCatalogueQueries, InventoryCatalogue.InventoryCatalogueQueries, VirtualCatalogue.VirtualCatalogueQueries)

  val Products = scenario("Categories, Standard And Variant Products")
    .exec(Category.CategoryQueries, ProductQuery.StandProductQueries, ProductQuery.VariantProductQueries)

  val Orders = scenario("Orders Related queries")
    .exec(Order.OrderQueries, Comment.CommentQueries, Consignment.ConsignmentQueries, Fulfilment.FulfilmentQueries,Article.ArticleQueries)

  val GI = scenario("Controls, Inventory Postions, Inventory Quantities")
    .exec(Control.ControlQueries, InventoryPositionQuery.InventoryPositionQueries,InventoryQuantity.InventoryQuantitesQueries,VirtualPosition.VirtualPositionQueries)

  val Others = scenario("Fulfilment Options, Users, And Permissions")
    .exec(FulfilmentOptionsQuery.FulfilmentOptionQueries, User.UserQueries, Roles.RoleQueries)


  val AllQueries = scenario("Queries With Auth")
    .exec(Oauth.JsonRetailerAuth, NetworksAndLocations, Catalogues, Products, Orders, GI, Others)
    .exec(Oauth.JsonRetailerAuth, Order.OrderQueries, ProductQuery.StandProductQueries, ProductQuery.VariantProductQueries,
          InventoryPositionQuery.InventoryPositionQueries, InventoryQuantity.InventoryQuantitesQueries,VirtualPosition.VirtualPositionQueries)


  setUp(
    // AllQueries.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds))
    NetworksAndLocations.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    Catalogues.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    Products.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    Orders.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    GI.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds)),
    Others.inject(rampUsers(Configurations.users) during(Configurations.rampUserDuration seconds))
  ).protocols(httpConfiguration)

 .throttle
         reachRps(100) in (10 seconds)
             holdFor(20 seconds)
                 jumpToRps(150)
                     holdFor(120 seconds)
       .assertions(
         // Assert that the max response time of all requests is less than 1500 ms
         global.responseTime.max.lt(1500),
         // Assert that every request has no more than 10% of failing requests
         forAll.failedRequests.percent.lte(10)
       )
       .maxDuration(Configurations.durations seconds)

}
