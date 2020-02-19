package com.sample;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,kafka-2:9092,kafka-3:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "simple-consumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"); // 300000 = 5 min by default
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.LongDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);
        System.out.println("Subscribing to `sample` topic");

        ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
                System.out.println("revoked"+ revokedPartitions.size() +"partitions");
                for (TopicPartition p : revokedPartitions) {
                    System.out.println("revoked partition:  " + p.topic() + ":" + p.partition());
                }
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
                System.out.println("assigned"+ assignedPartitions.size() +"partitions");
                for (TopicPartition p : assignedPartitions) {
                    System.out.println("assigned partition:  " + p.topic() + ":" + p.partition());
                }
            }
        };

        consumer.subscribe(Pattern.compile("vf_workshop.*"));
        long lastKey = 0L;
        while (true) {
            ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(100));
            if (!records.isEmpty() && records.count() != 1) {
                System.out.println("Received " + records.count());
            }
            for (ConsumerRecord<Long, String> record : records) {

                if (record.key() - lastKey != 1L) {
                    System.out.println("---");
                    System.out.println("KEY GAP FROM " + lastKey + " to " + record.key());
                }
                lastKey = record.key();

                System.out.println("Received offset = " + record.offset() + ", key = " + record.key() + ", value = "
                        + record.value());
            }
            try {
                consumer.commitSync();
            } catch (Exception e) {
                System.out.println("failed to commit: " + e.getMessage());
            }
        }
    }
}
