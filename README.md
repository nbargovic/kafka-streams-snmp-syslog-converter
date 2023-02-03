# kafka-streams-snmp-syslog-converter

## Work in progress

### Required Environment
* Java 11
* Confluent Platform 6.1 or newer

## Build
Use Maven to build the KStream Application.

```
mvn clean package
```

A successful build will create a target directory with the following two jar files:
* kafka-streams-snmp-syslog-converter-1.0.0.jar
* kafka-streams-snmp-syslog-converter-1.0.0-jar-with-dependencies.jar


The `kafka-streams-snmp-syslog-converter-1.0.0-jar-with-dependencies.jar` file contains all the dependencies needed to run the application. Therefore, this dependencies jar file should be the file executed when running the KStream Application.

## Configuration

Example:
```
application.id=filebeats-message-extractor
bootstrap.servers=kafka.fios-router.home:9092
security.protocol=PLAINTEXT

# topic and table name configuration
input.topic.name=snmp-input-data
error.topic.name=snmp-error-data
syslog.topic.name=syslog-output-data
vi.topic.name=snmp-v1-data
```

### Other Notes

kcat -b kafka.fios-router.home:9092 -t snmp-source-data -D# -K+ -P -l snmp.input

https://datatracker.ietf.org/doc/html/rfc5675
