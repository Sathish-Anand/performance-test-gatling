package util

import java.util

object Environment {
  val baseURL =System.getProperty("baseURL", "http://localhost:9090")
}
