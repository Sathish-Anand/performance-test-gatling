
**GATLING:**

Gatling is a powerful open-source tool for performance/load testing.

This performance test is to run different entities in graphql api parralelly and measure the performance.

**Implementation:**

maven-shade-plugin is used the compile the project and create a jar of whole project which can be run independetly without any dependencies.

**How To Run:**

*Project Folder*
~/src/main/scala/dataSetup/GraphqlQueriesTest.scala


*To Run the GQL Queries Test:*\
 `  mvn clean install -Dgatling.simulationClass=dataSetup.dataSetup.GraphQLQueriesTest -users=1 -rampUserDuration=2 -DbaseURL=http://test.api.facebook.com -Daccount=SATHISH
 `

 To Run the Retailer Setup.\
 `   mvn clean install -Daccount=DDDD48 -DbaseURL=http:///test.performance.facebook.com -DnoOfRetailer=3 -DnoOfNetwork=2 -DnoOfLocation=3 -DnoOfCategory=4 -DtotalStandardProduct=2 -DtotalProductPerStandard=4 -DtotalStandardWithoutChild=4 -DtotalVariantWithoutParent=4 -DreferenceStartsWith=ADA -Dusers=1 -Daccount=DDDD48
  `
  
*To Run the Product and Inventory:*\
` mvn clean gatling:test -DbaseURL=http://test.performance.facebook.com -DnoOfRetailer=3 -DnoOfNetwork=2 -DnoOfLocation=3 -DnoOfCategory=4 -DtotalStandardProduct=2 -DtotalProductPerStandard=4 -DtotalStandardWithoutChild=4 -DtotalVariantWithoutParent=4 -DreferenceStartsWith=ADA -Dusers=1 -Daccount=DDDD48
`

-DfromUser = (Start the test with this number of user)\
-DtoUser = (Max user to use or throughtout)\
-Dduration = Total time period  to run the test in Seconds.\
-DrampUserDuration = Ramp the user slowly to given timeperiod\.

**Result:**

Html file for the test will be saved under the ```~/result/index.html```
