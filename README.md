# kafka-streams-snmp-syslog-converter

## Work in progress

### Required Environment
* Java 11
* Confluent Platform 6.1 or newer
* Docker
* Kubernetes

## Build and Deploy
Use Maven to build the KStream Application. Push the docker image to a registry. Deploy with kubectl.

```
docker pull registry.access.redhat.com/ubi8/openjdk-11:1.11
mvn clean package install
docker push bargovic/snmp-syslog-stream:0.0.1
kubectl apply -f kstreams-deployment.yaml -n confluent
```

## Configuration

Example:
```
application.id=snmp-syslog-convert
bootstrap.servers=kafka.fios-router.home:9092
security.protocol=PLAINTEXT

input.topic.name=snmp-source-data
error.topic.name=snmp-syslog-convert-errors
v1.topic.name=snmp-v1-data
syslog.topic.name=syslog-output-data
v2.field.name=sysUpTime
```

### Other Notes

kcat -b kafka.fios-router.home:9092 -t snmp-source-data -D# -K+ -P -l snmp.input

https://datatracker.ietf.org/doc/html/rfc5675
