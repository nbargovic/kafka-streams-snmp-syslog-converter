package io.confluent.streams.siem;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Properties;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SnmpSyslogStream {

    private static final Logger log = LoggerFactory.getLogger(SnmpSyslogStream.class);

    /** default constructor */
    private SnmpSyslogStream() {
    }

    /** main method */
    public static void main(String[] args) throws IOException{
        if (args.length < 1) {
            throw new IllegalArgumentException("This program takes one argument: the path to an environment configuration file.");
        }

        new SnmpSyslogStream().run(args[0]);
    }

    /**
     * Run method that handles the life cycle of the Kafka Streams app.
     * @param configPath
     * @throws IOException
     */
    private void run(String configPath) throws IOException {

        Properties envProps = this.loadEnvProperties(configPath);
        Properties streamProps = this.buildStreamsProperties(envProps);

        Topology topology = this.buildTopology(envProps);

        final KafkaStreams streams = new KafkaStreams(topology, streamProps);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            streams.cleanUp();
            streams.start();
            log.info("Stream successfully started.");
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Setup the Streams Processors we will be using from the passed in configuration.properties.
     * @param envProps Environment Properties file
     * @return Properties Object ready for KafkaStreams Topology Builder
     */
    protected Properties buildStreamsProperties(Properties envProps) {
        Properties props = new Properties();
        props.putAll(envProps);

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());
        props.put(StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG, DefaultProductionExceptionHandler.class.getName());

        props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

        props.put(StreamsConfig.MAIN_CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");

        return props;
    }

    /**
     * Load in the Environment Properties that were passed in from the CLI.
     * @param fileName
     * @return Java properties object
     * @throws IOException
     */
    protected Properties loadEnvProperties(String fileName) throws IOException {
        Properties envProps = new Properties();

        try (
          FileInputStream input = new FileInputStream(fileName);
        ) {
            envProps.load(input);
        }
        return envProps;
    }

    /**
     * Build the topology from the loaded configuration
     * @param envProps built by the buildStreamsProperties
     * @return The build topology
     */
    static Topology buildTopology(Properties envProps) {

        //get configured properties
        final String inputTopicName = envProps.getProperty("input.topic.name");
        final String errorTopicName = envProps.getProperty("error.topic.name");
        final String v1TopicName = envProps.getProperty("v1.topic.name");
        final String sylogTopicName = envProps.getProperty("syslog.topic.name");
        final String v2FieldName = envProps.getProperty("v2.field.name");

        //create serde
        Map<String, Object> serdeProps = new HashMap<>();
        final Serializer<JsonNode> serializer = new JsonPOJOSerializer<>();
        serdeProps.put("JsonPOJOClass", JsonNode.class);
        serializer.configure(serdeProps, false);

        final Deserializer<JsonNode> deserializer = new JsonPOJODeserializer<>();
        serdeProps.put("JsonPOJOClass", JsonNode.class);
        deserializer.configure(serdeProps, false);

        final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(serializer, deserializer);
        StreamsBuilder builder = new StreamsBuilder();

        //branch v1 and v2 data
        KStream<JsonNode, JsonNode>[] branches = builder.stream(inputTopicName, Consumed.with(jsonSerde, jsonSerde))
                .branch(
                        (id, value) -> Objects.nonNull(value.get(v2FieldName)), //v2
                        (id, value) -> true                                     //v1
                );

        //v1 data to its own topic
        branches[1].to(v1TopicName, Produced.with(jsonSerde, jsonSerde));

        //v2 data gets transformed to syslog
        branches[0].map( (key, snmpData) -> {
            JsonNode snmpObject = (JsonNode)snmpData;
            JsonNode syslogObject = SnmpSyslogConverter.convert(snmpObject);
            return KeyValue.pair(key, syslogObject);
        }).to(sylogTopicName, Produced.with(jsonSerde, jsonSerde));

        return builder.build();
    }

}