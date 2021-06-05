package authentication

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.util.Properties
import com.github.javafaker.Faker
import org.json4s.DefaultFormats


object SuperAuthentication extends Simulation {

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

  val SuperAuth =
  {
      feed(feeder())
    .exec(http("Super Authentication")
      .post("/oauth/token")
      .queryParam("username", "${userAuth.username}")
      .queryParam("password", "${userAuth.password}")
      .queryParam("client_id", "${userAuth.client_id}")
      .queryParam("client_secret", "${userAuth.client_secret}")
      .queryParam("scope", "${userAuth.scope}")
      .queryParam("grant_type", "password")
      .check(status.is(200))
      .check(jsonPath("$.access_token").saveAs("superAuth"))
      .check(jsonPath("$.FirstName").saveAs("client_id"))
    )
  }

}
