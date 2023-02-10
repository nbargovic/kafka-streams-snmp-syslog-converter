FROM registry.access.redhat.com/ubi8/openjdk-11:1.11
WORKDIR /
COPY target/kafka-streams-snmp-syslog-converter-1.0.0-jar-with-dependencies.jar /
COPY src/main/configuration/dev.properties /
CMD ["java","-jar","kafka-streams-snmp-syslog-converter-1.0.0-jar-with-dependencies.jar","dev.properties"]