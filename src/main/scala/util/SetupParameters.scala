package util

import java.util

object SetupParameters {
  val stringPrefix = System.getProperty("referenceStartsWith", "test")
  val account = Integer.getInteger("noOfAccount", 1).toInt
  val retailer = Integer.getInteger("noOfRetailer", 2).toInt
  val network  = Integer.getInteger("noOfNetwork", 1).toInt
  val location  = Integer.getInteger("noOfLocation", 2).toInt
  val category  = Integer.getInteger("noOfCategory", 2).toInt
  // number of iterations that each scenario should run for non GI entities
//  val iterationCount: Int  = Integer.valueOf(System.getProperty("iterationCount", "50"))
  val standardProduct = Integer.getInteger("totalStandardProduct", 2).toInt
  val standardVariantProduct  = Integer.getInteger("totalProductPerStandard", 4).toInt
  val standardOnlyProduct  = Integer.getInteger("totalStandardWithoutChild", 2).toInt
  val variantOnlyProduct  = Integer.getInteger("totalVariantWithoutParent", 2).toInt
  val accountName = System.getProperty("account", "SAT15")
}
