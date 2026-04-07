# Topic Creation Guide - Kafka Metamorphosis

## 🪲 How to Create Topics

### Method 1: Using the admin namespace (Recommended)

```clojure
(require '[kafka-metamorphosis.admin :as admin])

;; Create an admin client
(def admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"}))

;; Create a simple topic
(admin/create-topic! admin-client "my-topic")

;; Create a topic with specific configurations
(admin/create-topic! admin-client "advanced-topic"
                     {:partitions 5
                      :replication-factor 1
                      :configs {"cleanup.policy" "compact"
                               "retention.ms" "86400000"}})

;; Check if a topic exists
(admin/topic-exists? admin-client "my-topic")

;; Create only if it doesn't exist
(admin/create-topic-if-not-exists! admin-client "safe-topic")

;; List all topics
(admin/list-topics admin-client)

;; Get topic details
(admin/describe-topic admin-client "my-topic")

;; Close the client
(admin/close! admin-client)
```

### Method 2: Using development utilities

```clojure
(require '[kafka-metamorphosis.dev :as dev])

;; Create a development topic (3 partitions, replication 1)
(dev/setup-dev-topic "dev-topic")

;; Create with custom options
(dev/setup-dev-topic "custom-topic" {:partitions 10})

;; List all topics
(dev/list-all-topics)

;; Describe a topic
(dev/describe-dev-topic "dev-topic")

;; Delete a topic (with confirmation)
(dev/delete-dev-topic "dev-topic")

;; View cluster information
(dev/cluster-info)
```

### Method 3: Command Line (Kafka Tools)

If you prefer to use native Kafka tools:

```bash
# Create a topic
kafka-topics.sh --create --topic my-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe a topic
kafka-topics.sh --describe --topic my-topic --bootstrap-server localhost:9092

# Delete a topic
kafka-topics.sh --delete --topic my-topic --bootstrap-server localhost:9092
```

## 📊 Common Topic Configurations

### Topic for Logs (Time-based Retention)

```clojure
(admin/create-topic! admin-client "logs-topic"
                     {:partitions 6
                      :configs {"retention.ms" "604800000"        ; 7 days
                                "cleanup.policy" "delete"
                                "segment.ms" "86400000"}})        ; 1 day
```

### Compacted Topic (for State)

```clojure
(admin/create-topic! admin-client "state-topic"
                     {:partitions 3
                      :configs {"cleanup.policy" "compact"
                                "min.cleanable.dirty.ratio" "0.1"
                                "delete.retention.ms" "86400000"}})
```

### High Throughput Topic

```clojure
(admin/create-topic! admin-client "high-throughput-topic"
                     {:partitions 12
                      :configs {"batch.size" "65536"
                                "linger.ms" "5"
                                "compression.type" "snappy"}})
```

## 🔧 Important Tips

1. **Partitions**: More partitions = greater parallelism, but also more overhead
2. **Replication**: For production, use replication-factor >= 3
3. **Compaction**: Use `cleanup.policy=compact` for state/unique key topics
4. **Retention**: Configure `retention.ms` based on business requirements
5. **Compression**: Use `compression.type=snappy` or `lz4` to save space

## 🚀 Complete Example

```clojure
(require '[kafka-metamorphosis.admin :as admin]
         '[kafka-metamorphosis.producer :as producer]
         '[kafka-metamorphosis.consumer :as consumer])

;; 1. Create admin client
(def admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"}))

;; 2. Create topic if it doesn't exist
(admin/create-topic-if-not-exists! admin-client "complete-example"
                                   {:partitions 3})

;; 3. Create producer and send message
(def p (producer/create {:bootstrap-servers "localhost:9092"
                         :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                         :value-serializer "org.apache.kafka.common.serialization.StringSerializer"}))

(producer/send! p "complete-example" "key1" "Hello, Kafka!")

;; 4. Create consumer and read message
(def c (consumer/create {:bootstrap-servers "localhost:9092"
                         :group-id "example-group"
                         :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                         :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"}))

(consumer/subscribe! c ["complete-example"])
(let [records (consumer/poll! c 5000)]
  (doseq [record records]
    (println "Received:" (:key record) "->" (:value record))))

;; 5. Clean up resources
(producer/close! p)
(consumer/close! c)
(admin/close! admin-client)
```

## 🦋 The Metamorphosis is Complete!

With these functions, you can easily manage Kafka topics using an idiomatic Clojure interface, transforming administration complexity into functional simplicity.
