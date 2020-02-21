package com.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SimpleProducer {
    public static void main(String[] args) throws InterruptedException {

        String topicName = (args.length == 0 ? "vf_workshop_1" : args[0]);

        System.out.println("started with args");
        for (String a : args) {
            System.out.println(a);
    }

        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092,kafka-2:9092,kafka-3:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        props.put(ProducerConfig.RETRIES_CONFIG, "10"); // default: Int.MAX_VALUE
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "1000"); // default: 120000
        // props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "prod-1");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        System.out.println("Sending data to"+ topicName +  "topic");
        try (Producer<Long, String> producer = new KafkaProducer<>(props)) {

            // init TX, otherwise sending will not work with idempotency
            // Cannot perform a 'send' before completing a call to initTransactions when transactions are enabled
            // producer.initTransactions();

            long i = 0;
            while (true) {
                ProducerRecord<Long, String> record = new ProducerRecord<>(topicName, i, "Value " + i);
                LocalDateTime now = LocalDateTime.now();
                System.out.println( now + "Sending " + record.key() + " " + record.value() + topicName);
                try {
                    RecordMetadata rm = producer.send(record).get(100, TimeUnit.MILLISECONDS);
                    System.out.println("success. offset: " + rm.offset());
                } catch (Exception e) {
                    System.out.println(now + "failed sending " + record.key() + ": " + e.getMessage());
                }
                i++;
                TimeUnit.MILLISECONDS.sleep(1);
            }
        }
    }
}
