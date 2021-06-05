package authentication

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.util.Properties
import com.github.javafaker.Faker


object testAuth extends Simulation {

    def get(): Map[String, Any] = {
          Map(
            "username" -> "facebook",
            "password" -> "66dfskjs-asdkj-sdfas",
            "client_id" -> "fcebook",
            "grant_type" -> "password",
            "scope" -> "facebook",
            "client_secret" -> "01mjasdjkhgsdmksdf"
          )
        }

   def feeder(): Iterator[Map[String,Any]] ={
        Iterator.continually(Map("userAuth"-> get()))
   }

   val authAcc =
  {
    feed(feeder())
      .exec(http("test Account Authentication")
        .post("/oauth/token")
        .queryParam("username", "${accUsername}")
        .queryParam("password", "${accPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "${scope}")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("accAuth"))
        .check(jsonPath("$.FirstName").saveAs("accName"))
      )
  }

  val authRet =
  {
    feed(feeder())
      .exec(http("test Retailer Authentication")
        .post("/oauth/token")
        .queryParam("username", "${retUsername}")
        .queryParam("password", "${retPassword}")
        .queryParam("client_id", "${clientId}")
        .queryParam("client_secret", "${clientSecret}")
        .queryParam("scope", "${scope}")
        .queryParam("grant_type", "password")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("retAuth"))
        .check(jsonPath("$.Retailer_id").saveAs("retId"))
        .check(jsonPath("$.FirstName").saveAs("retName"))
      )
  }


}
