package entities

import io.gatling.core.Predef.{StringBody, exec, _}
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._
import io.gatling.http.Predef.{http, status, _}
import util.{Configurations, Environment}
import java.io.FileWriter
import com.github.javafaker.Faker
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

object Retailer extends Simulation  {

  var accountId="";
  var name="";
  var address="";
  var phone="";
  var email="";

  implicit val formats = DefaultFormats


  def get(): Map[String, Any] = {
      var faker = new Faker();
     /*
      val r = new Retailer();
      r.name = faker.name().username();
      r.accountId = faker.animal().name()
      r.address = faker.address().fullAddress();
      r.phone = faker.phoneNumber().phoneNumber()
      r.email = faker.internet().emailAddress();
      r*/

      Map(
        "name" -> faker.name().username(),
        "accountId" -> faker.animal().name(),
        "address" -> faker.address().fullAddress(),
        "phone" -> faker.phoneNumber().phoneNumber(),
        "account" -> Map("id"->56)
      )
    }

  def feeder(): Iterator[Map[String,Any]] ={
    Iterator.continually(Map("retailer"-> get()))
    //Iterator.continually(Map("retailer" ->get(), "foo"->42))
  }
  val Create = exec(
    http("Test http")
      .post("/api/v4.1/job/${retailer.name}/${retailer.account.id}")
      .header("fluent.account" ,"TDEMO12")
      .header("Content-Type","application/json")
      .body(StringBody(write("${retailer}")))
  )
//    .exec(session => {
//        println("RETAILER:" + session)
//        session
//      }
//  )


//  val Create = exec(
//    { session =>
//        println("RETAILER:"+session)
//        session
//      }
//      http("Test http")
//      .get("/api/v4.1/job/${retailer.accountId}")
//  )



  override def toString = "Retailer($accountId, $name, $address, $phone, $email)"
}
/*

  Entity extends Simulation
      get() - creates the sample data from faker
      feeder() -  using the get/faker method have a gatling feeder for this entity

      Create - The actual gatling scenario
      SearchByBlah



  val accountCreation = scenario("Virtual View Query Performance Test With OnHand and Aggregate")
    .feed(Account.feeder)
    .asLongAs(_ => feederCount.getAndIncrement() <= noOfAccount) {
        exec(Account.Create())
          .feed(Retailer.feeder)
            .exec(Retailer)
  //      .exec(JobAndBatch.JobCreation)
    }

 val accountCreation = scenario("Virtual View Query Performance Test With OnHand and Aggregate")
    .feed(Account.feeder(noOfAccount))
        exec(Account.Create())
          .feed(Retailer.feeder(retailersByAccount))
            .exec(Retailer.Create())
                .feed(




 */