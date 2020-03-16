package com.sample;

import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleConsumer {

    private static final String KAFKA_ENV_PREFIX = "KAFKA_";

    public static void main(String[] args) throws ParseException {

        SimpleConsumerOptions opts = new SimpleConsumerOptions(args);

        Properties props = buildProperties(defaultProps, System.getenv(), KAFKA_ENV_PREFIX);
        System.out.println("creating consumer with props:");
        props.forEach( (k, v) -> System.out.println(k + ": " + v));

        KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);

        System.out.println("Subscribing to " + opts.topicName + " prefix");
        consumer.subscribe(Pattern.compile(opts.topicName), listener);
        long lastKey = 0L;
        while (true) {
            ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(100));

            LocalDateTime now = LocalDateTime.now();

            if (opts.verbose && !records.isEmpty() && records.count() != 1) {
                System.out.println(now + " Received " + records.count());
            }
            for (ConsumerRecord<Long, String> record : records) {
                String rp = record.topic() + "#" + record.partition();

                if (opts.checkGaps && record.key() - lastKey != 1L) {
                    System.out.println("---");
                    System.out.println(now + " KEY GAP on " + rp + "FROM " + lastKey + " to " + record.key());
                }
                lastKey = record.key();

                if (opts.verbose)
                    System.out.println(now + " Received " + rp + " offset = " + record.offset() + ", key = "
                            + record.key() + ", value = " + record.value());
            }
            try {
                consumer.commitSync();
            } catch (Exception e) {
                System.out.println(now + "failed to commit: " + e.getMessage());
            }
        }
    }

    private static ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
            System.out.println("rebalance: revoked " + revokedPartitions.size() + " partitions");
            for (TopicPartition p : revokedPartitions) {
                System.out.println("rebalance: revoked  " + p.topic() + ":" + p.partition());
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
            System.out.println("rebalance: assigned " + assignedPartitions.size() + " partitions");
            for (TopicPartition p : assignedPartitions) {
                System.out.println("rebalance: assigned  " + p.topic() + ":" + p.partition());
            }
        }
    };

    private static Map<String, String> defaultProps = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,kafka-2:9092,kafka-3:9092",
            ConsumerConfig.GROUP_ID_CONFIG, "simple-consumer",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

    private static Properties buildProperties(Map<String, String> baseProps, Map<String, String> envProps, String prefix) {
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
}
