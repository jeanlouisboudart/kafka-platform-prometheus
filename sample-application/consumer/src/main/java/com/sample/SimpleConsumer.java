package com.sample;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092,kafka-2:9092,kafka-3:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "simple-consumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"); // 300000 = 5 min by default
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.LongDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);
        System.out.println("Subscribing to `sample` topic");
        consumer.subscribe(Arrays.asList("sample"));
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
