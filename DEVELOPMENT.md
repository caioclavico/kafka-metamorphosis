# Development Setup

## Kafka Metamorphosis Development Guide

### Prerequisites

1. **Java 8 or higher**
2. **Leiningen** (Clojure build tool)
3. **Apache Kafka** (for testing)

### Installation

#### Windows with WSL

If you're using WSL (Windows Subsystem for Linux), install the prerequisites in your WSL environment:

```bash
# Update package list
sudo apt update

# Install Java
sudo apt install openjdk-11-jdk

# Install Leiningen
curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod +x lein
sudo mv lein /usr/local/bin/
lein --version

# Install Kafka (optional, for testing)
wget https://downloads.apache.org/kafka/2.8.1/kafka_2.13-2.8.1.tgz
tar -xzf kafka_2.13-2.8.1.tgz
```

#### macOS

```bash
# Install Java
brew install openjdk@11

# Install Leiningen
brew install leiningen

# Install Kafka (optional)
brew install kafka
```

#### Linux

```bash
# Install Java
sudo apt install openjdk-11-jdk  # Ubuntu/Debian
# or
sudo yum install java-11-openjdk  # CentOS/RHEL

# Install Leiningen
curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod +x lein
sudo mv lein /usr/local/bin/

# Install Kafka
# Download from https://kafka.apache.org/downloads
```

### Development Workflow

1. **Clone and setup**

   ```bash
   cd /path/to/kafka-metamorphosis
   lein deps  # Download dependencies
   ```

2. **Run tests**

   ```bash
   lein test
   ```

3. **Start REPL**

   ```bash
   lein repl
   ```

4. **Build JAR**
   ```bash
   lein uberjar
   ```

### Testing with Kafka

To test the library with a real Kafka instance:

1. **Start Zookeeper**

   ```bash
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```

2. **Start Kafka**

   ```bash
   bin/kafka-server-start.sh config/server.properties
   ```

3. **Create test topic**

   ```bash
   bin/kafka-topics.sh --create --topic metamorphosis-topic --bootstrap-server localhost:9092
   ```

4. **Run examples**
   ```clojure
   (require '[kafka-metamorphosis.examples :as examples])
   (examples/run-examples)
   ```

### REPL Development

```clojure
;; Start REPL and load the library
(require '[kafka-metamorphosis.producer :as producer])
(require '[kafka-metamorphosis.consumer :as consumer])

;; Create a producer (requires running Kafka)
(def p (producer/create {:bootstrap-servers "localhost:9092"
                         :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                         :value-serializer "org.apache.kafka.common.serialization.StringSerializer"}))

;; Send a test message
(producer/send! p "test-topic" "key1" "Hello from REPL!")

;; Close producer
(producer/close! p)
```

### File Structure

```
src/kafka_metamorphosis/
├── core.clj           # Main namespace and exports
├── producer.clj       # Producer functions
├── consumer.clj       # Consumer functions
├── util.clj          # Utility functions
└── examples.clj      # Usage examples

test/kafka_metamorphosis/
├── core_test.clj     # Core tests
└── producer_test.clj # Producer tests
```

### Next Steps

The basic wrapper is now implemented with:

- ✅ Producer creation and message sending (sync/async)
- ✅ Consumer creation and message polling
- ✅ Configuration utilities (map -> properties conversion)
- ✅ Kebab-case to dot notation conversion
- ✅ Basic error handling
- ✅ Resource management (close functions)

Future enhancements:

- Schema Registry support
- Kafka Streams wrapper
- Administrative operations
- Metrics integration
- Transaction support
