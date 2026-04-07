# KRaft Mode - Kafka without Zookeeper

## 🆕 What is KRaft?

KRaft (Kafka Raft) is the new Apache Kafka architecture that **eliminates the Zookeeper dependency**. Available since Kafka 2.8 and stable since 3.3.

### ✅ KRaft Advantages

- **🚀 Faster startup** - Fewer components to initialize
- **📦 Less complexity** - One less component to manage
- **🔧 Simplified configuration** - No need to configure Zookeeper
- **⚡ Better performance** - Metadata managed directly by Kafka
- **🎯 Future of Kafka** - Zookeeper will be discontinued

### ❌ Current Limitations

- **🧪 Relatively new** - Less time in production
- **📚 Less documentation** - Community still migrating
- **🔌 Some tools** - May not fully support it yet

## 🚀 How to Use in Kafka Metamorphosis

### Complete KRaft Mode

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
;; Minimalist setup - super fast
(dev/kafka-setup-simple!)

;; Just start Kafka without UI
(dev/kafka-up-simple!)
```

## 🏗️ KRaft vs Zookeeper Architecture

### Traditional (Zookeeper)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Zookeeper  │    │    Kafka    │    │  Kafka UI   │
│   :2181     │◄──►│   :9092     │◄──►│   :8080     │
└─────────────┘    └─────────────┘    └─────────────┘
```

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
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@localhost:29093"
      # ... without Zookeeper

  kafka-ui:
    # Web interface at localhost:8080
```

### Simple KRaft (`kraft-simple-docker-compose`)

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_PROCESS_ROLES: "broker,controller"
      # ... minimal configuration
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
- ✅ Want faster startup

### Use Zookeeper When:

- ⚠️ Critical production environment
- ⚠️ Legacy tools that don't support KRaft
- ⚠️ Compliance with existing setup

## 🦋 Future Migration

Kafka Metamorphosis facilitates migration:

```clojure
;; Currently using Zookeeper
(dev/kafka-setup-zookeeper!)

;; Migrate to KRaft (same API)
(dev/kafka-setup-kraft!)

;; Application code remains the same!
```

Kafka's metamorphosis eliminates Zookeeper complexity! 🪲➡️🦋
