# kafka-platform-prometheus

Simple demo of how to monitor Kafka Platform using Prometheus and Grafana.

# Prerequisites

You need to have docker and docker-compose installed.

# Getting started

```bash
git clone https://github.com/jeanlouisboudart/kafka-platform-prometheus.git
cd kafka-platform-prometheus
```

# Build local images
This repository contains some local docker images including :
* jmx_exporter
* a simple producer
* as simple consumer

To build all images you just need to run :

```bash
docker-compose build
```

# Start the environment
To start the environment simply run the following command
```bash
docker-compose up -d
```

Open a brower and visit http://localhost:3000 (grafana).
Login/password is admin/admin.

# Destroy the environment
To destroy the environment simply run the following command to destroy containers and associated volumes :
```bash
docker-compose down -v
```

# Advanced

## Create a topic

Create `demo-perf-topic` with 4 partitions and 3 replicas.

```bash
docker-compose exec kafka-1 bash -c 'KAFKA_OPTS="" kafka-topics --create --partitions 4 --replication-factor 3 --topic demo-perf-topic --zookeeper zookeeper-1:2181'
```

## Produces random messages

Open a new terminal window and generate random messages to simulate producer load.

```bash
docker-compose exec kafka-1 bash -c 'KAFKA_OPTS="" kafka-producer-perf-test --throughput 500 --num-records 100000000 --topic demo-perf-topic --record-size 100 --producer-props bootstrap.servers=localhost:9092'
```

## Consumes random messages

Open a new terminal window and generate random messages to simulate consumer load.

```bash
docker-compose exec kafka-1 bash -c 'KAFKA_OPTS="" kafka-consumer-perf-test --messages 100000000 --threads 1 --topic demo-perf-topic --broker-list localhost:9092 --timeout 60000'
```

