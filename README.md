# jmeter-Influxdb-backendlistener-client

# Overview
### Description
JMeter InfluxDB Backend Listener is a JMeter plugin enabling you to send test results to an InfluxDB engine. It is meant as an alternative live-monitoring tool to the built-in "InfluxDB" backend listener of JMeter with additional features enabled.

### Features

* Add the error details to failed samples for more debugging

* Change responseMessage for all sub samplers of Transaction controller and set them to Transaction controller response message (if it has any failed sub samplers)

* Filters
  * Only send the samples you want by using Filters! Simply type them as follows in the field ``SamplersList`` : ``filter1;filter2;filter3`` or ``samplename.*``.
  
* Optional tags
  * Splits a passed string to key-value pairs whereas a delimited by coma, colon or semicolon. Whereas key is a tag to be recorded in the InfluxDB-database and value is its value.
  * For ex. "testType=my_app_stress_test;testdataVersion=2.0" means that the InfluxDB gets two tags "testType" and "testdataVersion" with values.
  
* Continuous Integration support
  * For CI CD, users can track their load test through run id 

### Maven
```xml
<dependency>
  <groupId>com.github.venkatesan255</groupId>
  <artifactId>JmeterInfluxDBBackendListenerClient</artifactId>
  <version>1.2</version>
</dependency>
```


## Contributing
Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.

### Packaging and testing your newly added code
Execute below mvn command. Make sure JAVA_HOME is set properly
```
mvn clean install -Dgpg.skip=true
```
Move the resulting JAR to your `JMETER_HOME/lib/ext`.

 
