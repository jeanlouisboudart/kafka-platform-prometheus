package com.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SimpleProducer {

    private static final String KAFKA_ENV_PREFIX = "KAFKA_";

    public static void main(String[] args) throws InterruptedException {

        String topicName = (args.length == 0 ? "workshop_topic_1" : args[0]);
        System.out.println("producer args:");
        for (String a : args) {
            System.out.println(a);
        }

        Properties props = buildProperties(defaultProps, System.getenv(), KAFKA_ENV_PREFIX);
        System.out.println("creating producer with props:");
        props.forEach( (k, v) -> System.out.println(k + ": " + v));

        System.out.println("Sending data to topic " + topicName);
        try (Producer<Long, String> producer = new KafkaProducer<>(props)) {

            // init TX, otherwise sending will not work with idempotency
            // Cannot perform a 'send' before completing a call to initTransactions when transactions are enabled
            // producer.initTransactions();

            long id = 0;
            while (true) {
                ProducerRecord<Long, String> record = new ProducerRecord<>(topicName, id, "Value " + id);
                LocalDateTime now = LocalDateTime.now();
                System.out.println(now + "Sending " + record.key() + " " + record.value() + " to " + topicName);
                try {
                    RecordMetadata rm = producer.send(record).get(100, TimeUnit.MILLISECONDS);
                    System.out.println("succeeded sending. offset: " + rm.offset());
                } catch (Exception e) {
                    System.out.println(now + "failed sending " + record.key() + ": " + e.getMessage());
                }
                id++;
                TimeUnit.MILLISECONDS.sleep(1);
            }
        }
    }

    private static Map<String, String> defaultProps = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,kafka-2:9092,kafka-3:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

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
