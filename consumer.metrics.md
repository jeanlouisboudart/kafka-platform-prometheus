# consumer metrics explained

## bytes consumed rate

General performance metric per consumer.

## bytes consumed per topic

General performance metric per topic.

## rate of records consumed

General performance metric per consumer.

## rate of records consumed per topic

General performance metric per topic.

## average lag per topic

`kafka_consumer_consumer_fetch_manager_metrics_records_lag_avg{job="consumer"}`

Lag between the high watermark of a log and the consumer offset

A growing lag may mean underconsumption, perhaps because of a slow consumer.

Alerts may be useful. Exact values need to correspond to topic SLAs.

## average fetch size

## average fetch request latency

`kafka_consumer_consumer_fetch_manager_metrics_fetch_latency_avg`

relevant settings:

`fetch.min.bytes` - 1 by default
`max.partition.fetch.bytes` - 1MB by default
`fetch.max.wait.ms` 500ms by default.

A latency value close to the max.wait setting points to the max fetch bytes never 'winning'.

## average throttle time

If not `0` -> Quotas are enabled and requests are being throttled.

https://cwiki.apache.org/confluence/display/KAFKA/KIP-13+-+Quotas

https://cwiki.apache.org/confluence/display/KAFKA/KIP-124+-+Request+rate+quotas

## current number of active connections

kafka_consumer_consumer_metrics_connection_count

## lead

A lag metric between the consumer offset and the start offset of the log. If this latter lag gets close to 0, it's an indication that the consumer may lose data soon.

Data is being removed from the log faster then the consumer can consume it.

https://cwiki.apache.org/confluence/display/KAFKA/KIP-223+-+Add+per-topic+min+lead+and+per-partition+lead+metrics+to+KafkaConsumer

https://issues.apache.org/jira/browse/KAFKA-6184

## Q

// Q - what is this metric for: kafka_server_fetcherlagmetrics_consumerlag.

topic is `_consumer_offsets`
clientid=`ReplicaFetcherThread-X-Y`

// Q - failed authentication rate vs failed reauthentication rate

// Q - incoming bytes vs bytes consumed
