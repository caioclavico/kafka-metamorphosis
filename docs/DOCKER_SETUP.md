# Kafka with Docker - Development Guide

## 🐳 Quick Setup with Docker

Kafka Metamorphosis includes utility functions to facilitate local development using Docker with **3 different architectures**:

### 🏗️ Choose Your Architecture

#### **🆕 KRaft Mode (Recommended) - Kafka without Zookeeper**

```clojure
(require '[kafka-metamorphosis.dev :as dev])

;; Complete setup with KRaft (modern, faster)
(dev/kafka-setup-kraft!)

;; Or just start Kafka
(dev/kafka-up-kraft!)
```

#### **⚡ Simple KRaft (Fastest) - Minimal**

```clojure
;; Minimalist setup (no UI, super fast)
(dev/kafka-setup-simple!)

;; Or just start Kafka
(dev/kafka-up-simple!)
```

#### **🏛️ Traditional Mode - Kafka + Zookeeper**

```clojure
;; Traditional setup with Zookeeper
(dev/kafka-setup-zookeeper!)

;; Or just start Kafka
(dev/kafka-up-zookeeper!)
```

### 🚀 Generic Setup

```clojure
;; Generic method - choose the mode
(dev/kafka-dev-setup!)                    ; Default: Zookeeper
(dev/kafka-dev-setup! :kraft)             ; KRaft mode
(dev/kafka-dev-setup! :simple)            ; Simple KRaft
(dev/kafka-dev-setup! :kraft ["my-topic"]) ; KRaft with custom topics
```

### 📊 Mode Comparison

| Mode          | Containers            | Startup   | UI  | Zookeeper | Usage                    |
| ------------- | --------------------- | --------- | --- | --------- | ------------------------ |
| **KRaft**     | 2 (Kafka + UI)        | Medium    | ✅  | ❌       | Complete development     |
| **Simple**    | 1 (Kafka)             | Fast      | ❌  | ❌       | Quick tests              |
| **Zookeeper** | 3 (Zook + Kafka + UI) | Slow      | ✅  | ✅       | Legacy compatibility     |

### 🔧 Manual Control

```clojure
;; Start Kafka
(dev/kafka-docker-up!)

;; Wait for it to be ready
(dev/wait-for-kafka)

;; Check status
(dev/kafka-docker-status)

;; View logs
(dev/kafka-docker-logs!)
(dev/kafka-docker-logs! "kafka" true)  ; Follow Kafka logs

;; Restart services
(dev/kafka-docker-restart!)

;; Stop services
(dev/kafka-docker-down!)

;; Stop and remove volumes (cleans data)
(dev/kafka-docker-down! true)
```

### 🧹 Complete Cleanup

```clojure
;; Stop everything and keep data
(dev/kafka-dev-teardown!)

;; Stop everything and remove data
(dev/kafka-dev-teardown! true)
```

## 📊 Included Services

### Kafka Broker

- **Port**: 9092 (main)
- **Alternative port**: 29092
- **Address**: `localhost:9092`

### Zookeeper

- **Port**: 2181
- **Address**: `localhost:2181`

### Kafka UI

- **Port**: 8080
- **URL**: http://localhost:8080
- **Features**:
  - View topics
  - Monitor messages
  - Manage consumers
  - View metrics

## 🐳 Generated Docker Compose

The `docker-compose.yml` file includes:

```yaml
version: "3.8"

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: kafka-metamorphosis-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-metamorphosis-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-metamorphosis-ui
    depends_on:
      - kafka
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

## 🛠️ Workflow de Desenvolvimento

### 1. Primeira vez / Setup inicial

```clojure
;; Configure full environment
(dev/kafka-dev-setup!)
```

### 2. Daily development

```clojure
;; Check if it's running
(dev/kafka-docker-status)

;; If not, start it
(dev/kafka-docker-up!)

;; Work with topics and messages...
(dev/setup-dev-topic "my-project")
(dev/send-test-messages "my-project" 10)
(dev/read-test-messages "my-project")
```

### 3. Debug / Troubleshooting

```clojure
;; View Kafka logs
(dev/kafka-docker-logs! "kafka")

;; Restart if necessary
(dev/kafka-docker-restart! "kafka")

;; Check if can connect
(dev/wait-for-kafka 30)
```

### 4. End of work

```clojure
;; Stop but keep data
(dev/kafka-docker-down!)

;; Or clean everything
(dev/kafka-dev-teardown! true)
```

## 🐛 Troubleshooting

### Kafka won't start

```bash
# Check if Docker is running
docker --version

# Check occupied ports
netstat -an | grep ":9092"
```

### Error logs

```clojure
;; View all logs
(dev/kafka-docker-logs!)

;; View specific logs
(dev/kafka-docker-logs! "kafka")
(dev/kafka-docker-logs! "zookeeper")
```

### Clean corrupted state

```clojure
;; Stop everything and remove volumes
(dev/kafka-dev-teardown! true)

;; Recreate from scratch
(dev/kafka-dev-setup!)
```

## 🎯 Manual Docker Commands

If you need to run Docker commands directly:

```bash
# Start services
docker-compose up -d

# View status
docker-compose ps

# View logs
docker-compose logs kafka

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## 🦋 The Docker Metamorphosis is Complete!

With these functions, you can easily manage a local Kafka environment for development, transforming configuration complexity into functional simplicity! 🪲➡️🦋
