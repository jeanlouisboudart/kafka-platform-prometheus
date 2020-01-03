# The more important cluster monitoring points

1. [Active Controller count](#active-controller-count)
2. [Offline partitions](#offline-partitions)
3. [Unclean Elections](#unclean-elections)
4. [Under Replicated Partitions](#under-replicated-partitions)
5. [Under Min In Sync Replicas](#under-min-in-sync-replicas)
6. [Max lag by groupId by partition](#max-lag-by-groupid-by-partition)
7. [Broker IO Activity](#broker-io-activity)
8. [Broker Net Activity](#broker-network-activity)
9. [Zookeper Avg Latency](#zookeeper-avg-latency)
10. [Zookeper Connections](#zookeeper-connections)
11. [Broker Zookeeper disconnections](#broker-zookeeper-disconnections)

[Bonus: Why I don't have to monitor the number of brokers?](#bonus-why-i-dont-have-to-monitor-the-number-of-brokers)

## Active Controller count
* **Type**: Message Delivery
* **Description**: The Controller is responsible for maintaining the list of partition leaders, and coordinating leadership transitions (topic creation)
  * If `ActiveControllerCount < 1`: Producers/Consumers can't get the partition leaders anymore.
  * If `ActiveControllerCount > 1`: A [split-brain](https://en.wikipedia.org/wiki/Split-brain_(computing)) occurs, ans that's really bad!
* **Metric:**: Sum the JMX metric `kafka.controller:type=KafkaController,name=ActiveControllerCount` across the cluster.
_Note_: Each broker exposes the `ActiveControllerCount` metric where the value is `0` or `1` wether the node is a controller or not. So it looks like a boolean but you need to do an integer sum across the cluster.
* **Notification**:
  * Send a warning when `ActiveControllerCount != 1`
  * Send an alarm when `ActiveControllerCount != 1` for more than 10s.

## Offline partitions
* **Type**: Message Delivery
* **Description**:  An _Offline Partition_ is a partition without active leader and are hence not writable or readable. The presence of Offline partitions compromise the data integrity of the cluster.
* **Metric**:  JMX `kafka.controller:type=KafkaController,name=OfflinePartitionsCount`
* **Notification**: Alarm when `OfflinePartitionsCount > 0`

## Unclean Elections
* **Type**: Message Delivery
* **Description**: Normally, when a broker that is the leader for a partition goes offline, a new leader is elected from the set of ISRs for the partition. An unclean leader election is a special case in which no available replicas are in sync. Because each topic _must_ have a leader, an election is held among the out-of-sync replicas and a leader is chosen—meaning any messages that were not synced prior to the loss of the former leader are lost forever. Essentially, unclean leader elections sacrifice consistency for availability.
* **Metric**:  JMX `kafka.controller:type=ControllerStats,name=UncleanLeaderElectionsPerSec`
* **Notification**:
  * Notify when `UncleanLeaderElectionsPerSec > 0`
  * Alarm when `UncleanLeaderElectionsPerSec > 0` for more than 1min.

## Under Replicated Partitions
* **Type**: Message Delivery
* **Description**: In a healthy cluster, the number of in sync replicas (ISRs) should be exactly equal to the total number of replicas. In other words, the metric ensure the partitions are respecting the topic replication factor configuration. Partitions under replicated can appear when a cluster node is down.
* **Metric**: JMX `kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions`
* **Notification**:
  * Notify when `UnderReplicatedPartitions > 0`
  * Alarm when `UnderReplicatedPartitions > 0` for more than 1min.

## Under Min In Sync Replicas
* **Type**: Message Delivery
* **Description**:  If the cluster can't reach the `min.insync.replicas` and `acks` is set to all (because there is no enough broker up in the cluster for instance), the data producer can't receive the ack, it raises a timeout exception and may retry depending on its configuration). In a nutshell, when you have partition under min ISR is almost sure the the data production is blocked.
* **Metric**: JMX `kafka.server:type=ReplicaManager,name=UnderMinIsrPartitionCount`
* **Notification**: Alarm when `UnderMinIsrPartitionCount > 0`

## Max lag by groupid by partition
* **Type**: Performance
* **Description**: The _lag_ is the delta between the offset of the last data appended in a topic and the offset of the committed read of a consumer. The bigger the lag is, the slower the consumer is reading the data.
* **Metric**:
* **Notification**: N/A

## Broker IO activity
* **Type**: Performance
* **Description**: Internally a broker node use the `I/O Thread`to read a message from the `Request Queue`, write it to the OS page cache and place it into the `Purgatory` where your replication strategy will be executed. It's interesting to monitor the thread idle time ("Idle" means not active):
  * When `idle==1`: The broker is inactive, from a pure performance stand point it could be removed.
  * When `idle==0`: The broker is always processing, you should either increase the number of threads or add a new broker into the cluster.

_Note: Thread scaling should be done carefully since it can have huge impact on the overall node performance._
* **Metric**: JMX `kafka.network:type=SocketServer,name=NetworkProcessorAvgIdlePercent`
  *  **Notification**: Alarm when `RequestHandlerAvgIdlePercent < 0.4`

## Broker Network activity
* **Type**: Performance
* **Description**: Same as above, a broker node use the `Network Thread`to read a message from the network and place it into the `Request Queue`. It's interesting to monitor the thread idle ("Idle" means not active):
  * When `idle==1`: The broker has no inbound traffic, from a pure performance stand point it could be removed.
  * When `idle==0`: The broker is always receiving messages , you should either increase the number of threads or add a new broker into the cluster.

_Note: Thread scaling should be done carefully since it can have huge impact on the overall node performance._
* **Metric**: JMX `kafka.network:type=SocketServer,name=NetworkProcessorAvgIdlePercent`
  *  **Notification**: Alarm when `NetworkProcessorAvgIdlePercent < 0.4`

## Zookeper Avg Latency
* **Type**: Cluster Health
* **Description**: Zookeeper is a key part of the Kafka distributed capability, it is responsible of the Active Controller election, topic configuration, ACLs, broker membership in the cluster. In other word, if your Zookeeper cluster is unhealthy, your Kafka cluster will face issues soon.

The latency is the amount of time it takes for the server to respond to a client request. The value is dependent of the context (10ms is a standard reference), and should be stable over the time.
* **Metric**: JMX `AvgRequestLatency`
* **Notification**:
  * Notify when `AvgRequestLatency > 10ms`
  * Alarm when `AvgRequestLatency > 10ms` lasts for more than 1m.

## Zookeeper Connections
* **Type**: Cluster Health
* **Description**: Zookeeper has a limit of client connection it can handles (configured by `maxClientCnxns`). When the limit is reached, the new request will be dropped which can affect your Kafka service (controller election, ACLs, etc.)
* **Metric**: JMX `NumAliveConnections`
* **Notification**:  Alarm when `NumAliveConnections/maxClientCnxns > 0.7`

## Broker Zookeeper disconnections
* **Type**: Cluster health
*  **Description**: The current Broker has been  disconnected from the ensemble. The broker lost its previous connection to a server and it is currently trying to reconnect. The session is not necessarily expired. A high rate of disconnection is a symptom of network issues.
* **Metric**: JMX `kafka.server:type=SessionExpireListener,name=ZooKeeperDisconnectsPerSec`
* **Notification**: 
  * Notify when `ZooKeeperDisconnectsPerSec > X`

## Bonus: Why I don't have to monitor the number of brokers?
You can be surprised to not see the number of brokers has a monitoring point. Think about it, what can occur you loose a broker.
* The producers will be blocked because the min.isr criterias is not satisfied? The UnderMinInSyncReplicas alert will be triggered.
* You can't guarantee the durability of the messages: Under Replicated? The UnderReplicatedPartitions alert will be triggered.
* You will have performance issue? The BrokeActivity(IO and/or Net) will be trigered.

Your Kafka cluster is shaped to meet one or many of the Kafka key features (speed, durability, resilience, etc.). While having the number of brokers is a good information it's redundant details to build a health check monitoring vision. 
