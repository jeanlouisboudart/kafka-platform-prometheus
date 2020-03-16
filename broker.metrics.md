# broker metrics explained

## brokers online

`count(kafka_server_replicamanager_leadercount)`

Set the thresholds depending on your cluster size

// Q - why the leader count and not state or any other similar metric?

## active controller count

`sum(kafka_controller_kafkacontroller_activecontrollercount)`

`kafka.controller:type=KafkaController,name=ActiveControllerCount`

// TODO - how is this metric generated? looks like it can be wrong, whowing 2 controllers when in reality, there is a rapid change between controllers.

## unclean leader election rate

Unclean leader elections are an extreme measure to ensure availability at the cost of data consistency. Any value >0 is risky.

`sum(kafka_controller_controllerstats_uncleanleaderelections_total)`

## online partitions

`sum(kafka_server_replicamanager_partitioncount)`

// why not the controller metric?

kafka.controller:type=KafkaController,name=GlobalPartitionCount

## underreplicated partitions

## CPU usage

## CPU idle

## messages in per topic

we need to ignore the instance sine we need to know per topic
