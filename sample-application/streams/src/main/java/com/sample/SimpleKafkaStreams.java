package com.sample;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SimpleKafkaStreams {

    private static final String KAFKA_ENV_PREFIX = "KAFKA_";
    private final Logger logger = LoggerFactory.getLogger(SimpleKafkaStreams.class);
    private final Properties properties;
    private final String inputTopic;
    private final String outputTopic;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SimpleKafkaStreams simpleKafkaStreams = new SimpleKafkaStreams();
        simpleKafkaStreams.start();
    }

    public SimpleKafkaStreams() throws InterruptedException, ExecutionException {
        properties = buildProperties(defaultProps, System.getenv(), KAFKA_ENV_PREFIX);
        inputTopic = System.getenv().getOrDefault("INPUT_TOPIC","sample");
        outputTopic = System.getenv().getOrDefault("OUTPUT_TOPIC","output");

        final Integer numberOfPartitions =  Integer.valueOf(System.getenv().getOrDefault("NUMBER_OF_PARTITIONS","2"));
        final Short replicationFactor =  Short.valueOf(System.getenv().getOrDefault("REPLICATION_FACTOR","3"));

        AdminClient adminClient = KafkaAdminClient.create(properties);
        createTopic(adminClient, inputTopic, numberOfPartitions, replicationFactor);
        createTopic(adminClient, outputTopic, numberOfPartitions, replicationFactor);

    }

    private void start() {
        logger.info("creating streams with props: {}", properties);

        final StreamsBuilder builder = new StreamsBuilder();
        KTable<String, Long> count = builder.stream(inputTopic, Consumed.with(Serdes.Long(), Serdes.String()))
                .groupBy((key, value) -> key % 2 == 0 ? "even" : "odd", Grouped.with(Serdes.String(), Serdes.String()))
                .count();

        KStream<String, Long> stream = count
                .toStream();

        stream.print(Printed.toSysOut());
        stream.to(outputTopic, Produced.with(Serdes.String(),Serdes.Long()));
        Topology topology = builder.build();
        logger.info(topology.describe().toString());

        KafkaStreams streams = new KafkaStreams(topology,properties);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }


    private Map<String, String> defaultProps = Map.of(
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,localhost:19093,localhost:19094",
            StreamsConfig.APPLICATION_ID_CONFIG, System.getenv().getOrDefault("APPLICATION_ID","sample-streams"),
            StreamsConfig.REPLICATION_FACTOR_CONFIG, "3"
            );

    private Properties buildProperties(Map<String, String> baseProps, Map<String, String> envProps, String prefix) {
        Map<String, String> systemProperties = envProps.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey()
                                .replace(prefix, "")
                                .toLowerCase()
                                .replace("_", ".")
                        , e -> e.getValue())
                );

        Properties props = new Properties();
        props.putAll(baseProps);
        props.putAll(systemProperties);
        return props;
    }

    private void createTopic(AdminClient adminClient, String topicName, Integer numberOfPartitions, Short replicationFactor) throws InterruptedException, ExecutionException {
        if (!adminClient.listTopics().names().get().contains(topicName)) {
            logger.info("Creating topic {}", topicName);
            final NewTopic newTopic = new NewTopic(topicName, numberOfPartitions, replicationFactor);
            try {
                CreateTopicsResult topicsCreationResult = adminClient.createTopics(Collections.singleton(newTopic));
                topicsCreationResult.all().get();
            } catch (ExecutionException e) {
                //silent ignore if topic already exists
            }
        }
    }

}
