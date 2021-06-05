package util

object Configurations {
//  val users = scala.util.Properties.envOrElse("users", "2")
  val maxResponseTime = scala.util.Properties.envOrElse("maxResponseTime", "1000")
  val fromUsers = Integer.getInteger("fromUser", 2).toInt
  val toUsers = Integer.getInteger("toUser", 10).toInt
  val users = Integer.getInteger("users", 10).toInt
  val duration = Integer.getInteger("duration", 5).toInt
  val throughput = Integer.getInteger("throughput", 10).toInt
  val rampUserDuration = Integer.getInteger("rampUserDuration", 10).toInt

}
