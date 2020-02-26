package com.sample;

import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

public class SimpleConsumer {
    public static void main(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption("t", true, "topic name");
        options.addOption("g", false, "check gaps");
        options.addOption("v", false, "log batches and individual records");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        cmd.iterator().forEachRemaining(System.out::println);

        String tP = cmd.getOptionValue("t",  "vf_workshop") + ".*";
        boolean checkGaps = cmd.hasOption("g");
        boolean verbose = cmd.hasOption("v");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,kafka-2:9092,kafka-3:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "simple-consumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // default true if group.id is provided
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // default read_uncommitted
        // default: org.apache.kafka.clients.consumer.RangeAssignor
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"); // 300000 = 5 min by default
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.LongDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        System.out.println("starting consumer with following config");
        props.forEach((k, v) -> System.out.println(k + ": " + v));

        KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);

        System.out.println("Subscribing to " + tP + " prefix");

        ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
                System.out.println("rebalance: revoked "+ revokedPartitions.size() +" partitions");
                for (TopicPartition p : revokedPartitions) {
                    System.out.println("rebalance: revoked  " + p.topic() + ":" + p.partition());
                }
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
                System.out.println("rebalance: assigned "+ assignedPartitions.size() +" partitions");
                for (TopicPartition p : assignedPartitions) {
                    System.out.println("rebalance: assigned  " + p.topic() + ":" + p.partition());
                }
            }
        };

        consumer.subscribe(Pattern.compile(tP), listener);
        long lastKey = 0L;
        while (true) {
            ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(100));

            LocalDateTime now = LocalDateTime.now();

            if (verbose && !records.isEmpty() && records.count() != 1) {
                System.out.println(now + " Received " + records.count());
            }
            for (ConsumerRecord<Long, String> record : records) {
                String rp = record.topic() + "#" + record.partition();

                if (checkGaps && record.key() - lastKey != 1L) {
                    System.out.println("---");
                    System.out.println(now + " KEY GAP on " + rp + "FROM " + lastKey + " to " + record.key());
                }
                lastKey = record.key();

                if( verbose ) System.out.println(now + " Received " + rp + " offset = " + record.offset() + ", key = " + record.key() + ", value = "
                        + record.value());
            }
            try {
                consumer.commitSync();
            } catch (Exception e) {
                System.out.println(now + "failed to commit: " + e.getMessage());
            }
        }
    }
}
