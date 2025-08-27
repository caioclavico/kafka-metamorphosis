# Kafka Metamorphosis 🪲

![Kafka Metamorphosis](docs/images/kafka-metamorphosis.jpg)

_"When Gregor Samsa woke up one morning from unsettling dreams, he found himself changed in his bed into a monstrous vermin."_  
— Franz Kafka, The Metamorphosis

## Overview

Kafka Metamorphosis is a Clojure wrapper that transforms the Java Kafka driver into a friendly, idiomatic Clojure interface. Just as Gregor Samsa underwent his transformation, this library metamorphoses the complex Java APIs into elegant Clojure functions that feel natural to use.

## Features

- 🪲 **Idiomatic Clojure API** - Clean, functional interface over Kafka's Java driver
- 📦 **Producer & Consumer** - Simple functions for publishing and consuming messages
- ⚙️ **Configuration Made Easy** - Clojure maps instead of Java Properties
- 🔄 **Async Support** - Built-in support for asynchronous operations
- 🛡️ **Error Handling** - Proper exception handling with meaningful error messages
- 📊 **Monitoring Ready** - Easy integration with metrics and monitoring tools

## Installation

Add the following dependency to your `project.clj`:

```clojure
[kafka-metamorphosis "0.1.0-SNAPSHOT"]
```

Or for deps.edn:

```clojure
kafka-metamorphosis {:mvn/version "0.1.0-SNAPSHOT"}
```

## Quick Start

### Creating a Producer

```clojure
(require '[kafka-metamorphosis.producer :as producer])

(def producer-config
  {:bootstrap-servers "localhost:9092"
   :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "org.apache.kafka.common.serialization.StringSerializer"})

(def producer (producer/create producer-config))

;; Send a message
(producer/send! producer "my-topic" "my-key" "Hello, Kafka!")
```

### Creating a Consumer

```clojure
(require '[kafka-metamorphosis.consumer :as consumer])

(def consumer-config
  {:bootstrap-servers "localhost:9092"
   :group-id "my-consumer-group"
   :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"})

(def consumer (consumer/create consumer-config))

;; Subscribe to topics
(consumer/subscribe! consumer ["my-topic"])

;; Poll for messages
(consumer/poll! consumer 1000)
```

## Configuration

Kafka Metamorphosis accepts configuration as Clojure maps, automatically converting them to the Java Properties format expected by the underlying Kafka clients. All standard Kafka configuration options are supported.

### Common Producer Settings

```clojure
{:bootstrap-servers "localhost:9092"
 :acks "all"
 :retries 3
 :batch-size 16384
 :linger-ms 1
 :buffer-memory 33554432}
```

### Common Consumer Settings

```clojure
{:bootstrap-servers "localhost:9092"
 :group-id "my-group"
 :enable-auto-commit true
 :auto-commit-interval-ms 1000
 :session-timeout-ms 30000}
```

## Advanced Usage

### Async Producer

```clojure
(producer/send-async! producer "topic" "key" "value"
  (fn [metadata exception]
    (if exception
      (println "Error:" (.getMessage exception))
      (println "Message sent to" (.topic metadata) "partition" (.partition metadata)))))
```

### Consumer with Custom Processing

```clojure
(consumer/consume! consumer
  {:poll-timeout 1000
   :handler (fn [record]
              (println "Received:" (.value record))
              ;; Process the record
              )})
```

## Development

### Running Tests

```bash
lein test
```

### Building

```bash
lein uberjar
```

### REPL Development

```bash
lein repl
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for your changes
5. Run the test suite
6. Submit a pull request

## Roadmap

- [ ] Schema Registry support
- [ ] Kafka Streams wrapper
- [ ] Administrative operations
- [ ] Metrics integration
- [ ] Transaction support
- [ ] Comprehensive documentation

## Inspiration

This project draws inspiration from Franz Kafka's "The Metamorphosis," where the protagonist undergoes a dramatic transformation. Similarly, this library transforms the Java Kafka API into something more suitable for the Clojure ecosystem - a metamorphosis from imperative to functional, from complex to simple.

## Related Projects

- [Apache Kafka](https://kafka.apache.org/) - The underlying streaming platform
- [jackdaw](https://github.com/FundingCircle/jackdaw) - Another Clojure Kafka library
- [clj-kafka](https://github.com/pingles/clj-kafka) - Alternative Clojure Kafka client

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
