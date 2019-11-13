package com.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SimpleProducer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092,kafka-2:9092,kafka-3:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        System.out.println("Sending data to `sample` topic");
        try (Producer<Long, String> producer = new KafkaProducer<>(props)) {
            long i = 0;
            while (true) {
                ProducerRecord<Long, String> record = new ProducerRecord<>("sample", i, "Value " + i);
                System.out.println("Sending " + record.key() + " " + record.value());
                try {
                    RecordMetadata rm = producer.send(record).get();
                    System.out.println("success. offset: " + rm.offset());
                } catch (Exception e) {
                    System.out.println("failed sending " + record.key() + ": " + e.getMessage());
                }

                i++;
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }
}
