# KRaft Mode - The only mode in Kafka 4.x

## 🆕 What is KRaft?

KRaft (Kafka Raft) is the Apache Kafka architecture that **eliminates the Zookeeper dependency**. It is the default and **only** mode supported by Kafka 4.x — ZooKeeper was removed in Kafka 4.0.

### ✅ KRaft Advantages

- **🚀 Faster startup** - Fewer components to initialize
- **📦 Less complexity** - One less component to manage
- **🔧 Simplified configuration** - No Zookeeper to configure
- **⚡ Better performance** - Metadata managed directly by Kafka

## 🚀 How to Use in Kafka Metamorphosis

### Complete KRaft Mode (Kafka + UI)

```clojure
;; Complete setup with KRaft + Kafka UI
(dev/kafka-setup-kraft!)

;; Or with specific topics
(dev/kafka-setup-kraft! ["orders" "payments" "notifications"])

;; Just start without creating topics
(dev/kafka-up-kraft!)
```

### Simple KRaft Mode (Recommended for Tests)

```clojure
;; Minimalist setup - super fast, no UI
(dev/kafka-setup-simple!)

;; Just start Kafka without UI
(dev/kafka-up-simple!)
```

## 🏗️ Architectures

### KRaft Mode

```
┌─────────────┐    ┌─────────────┐
│    Kafka    │    │  Kafka UI   │
│   :9092     │◄──►│   :8080     │
│ (+ metadata)│    └─────────────┘
└─────────────┘
```

### Simple KRaft

```
┌─────────────┐
│    Kafka    │
│   :9092     │
│ (+ metadata)│
└─────────────┘
```

## ⚙️ Generated Configurations

### Complete KRaft (`kraft-docker-compose`)

```yaml
services:
  kafka:
    image: apache/kafka:4.3.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@localhost:29093"
      # Single-node replication overrides
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR: 1

  kafka-ui:
    # Web interface at localhost:8080
```

### Simple KRaft (`kraft-simple-docker-compose`)

```yaml
services:
  kafka:
    image: apache/kafka:4.3.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_PROCESS_ROLES: "broker,controller"
      # … minimal single-node configuration
```

## 🛠️ Recommended Workflow

### For Daily Development

```clojure
;; 1. Quick setup for development
(dev/kafka-setup-kraft!)

;; 2. Use normally
(dev/send-test-messages "my-topic" 5)
(dev/read-test-messages "my-topic")

;; 3. Open UI in browser: http://localhost:8080

;; 4. Stop when finished
(dev/kafka-dev-teardown!)
```

### For Quick Tests

```clojure
;; 1. Minimalist setup (no UI)
(dev/kafka-setup-simple!)

;; 2. Test quickly
(dev/setup-dev-topic "test")
(dev/send-test-messages "test" 3)

;; 3. Stop
(dev/kafka-dev-teardown!)
```

## 🔧 KRaft Troubleshooting

### Problem: Kafka won't start

```clojure
;; View specific logs
(dev/kafka-docker-logs! "kafka")

;; Check if ports are free
;; 9092 (Kafka), 29093 (Controller)
```

### Problem: Can't connect

```clojure
;; Wait for complete initialization
(dev/wait-for-kafka 60)

;; Check status
(dev/kafka-docker-status)
```

### Complete Reset

```clojure
;; Stop and clean everything
(dev/kafka-dev-teardown! true)

;; Recreate from scratch
(dev/kafka-setup-kraft!)
```

## 🎯 Recommendations

### Use KRaft When:

- ✅ Local development
- ✅ Automated tests
- ✅ New projects
- ✅ Any project targeting Kafka 4.x (mandatory)

## 🪲 Migration from ZooKeeper

If you used `kafka-setup-zookeeper!` in older versions, switch to KRaft — application code stays the same:

```clojure
;; Previously (Kafka 3.x)
;; (dev/kafka-setup-zookeeper!)

;; Now (Kafka 4.x — KRaft only)
(dev/kafka-setup-kraft!)
```

Kafka's metamorphosis eliminates Zookeeper complexity! 🪲➡️🦋
