;; Development configuration and helpers
(ns kafka-metamorphosis.dev
  "Development utilities and configurations"
  (:require [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.admin :as admin]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string]))

;; Common development configurations
(def local-kafka-config
  "Standard configuration for local Kafka development"
  {:bootstrap-servers "localhost:9092"})

(def string-producer-config
  "Producer config for string key/value serialization"
  (merge local-kafka-config
         {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
          :acks "all"
          :retries 3}))

(def string-consumer-config
  "Consumer config for string key/value deserialization"
  (merge local-kafka-config
         {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
          :auto-offset-reset "earliest"
          :enable-auto-commit true}))

;; Docker Compose configuration for Kafka
(def default-docker-compose
  "Default docker-compose.yml content for Kafka development"
  "version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: kafka-metamorphosis-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - \"2181:2181\"
    networks:
      - kafka-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-metamorphosis-kafka
    depends_on:
      - zookeeper
    ports:
      - \"9092:9092\"
      - \"29092:29092\"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
      KAFKA_LOG4J_LOGGERS: \"kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO\"
    networks:
      - kafka-network
    healthcheck:
      test: [\"CMD-SHELL\", \"kafka-topics --bootstrap-server localhost:9092 --list\"]
      interval: 30s
      timeout: 10s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-metamorphosis-ui
    depends_on:
      - kafka
    ports:
      - \"8080:8080\"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    networks:
      - kafka-network

networks:
  kafka-network:
    driver: bridge
")

;; KRaft (Kafka without Zookeeper) configuration
(def kraft-docker-compose
  "Docker compose configuration for Kafka with KRaft (no Zookeeper)"
  "version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-metamorphosis-kraft
    ports:
      - \"9092:9092\"
      - \"29092:29092\"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@localhost:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:29093,PLAINTEXT_HOST://0.0.0.0:29092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
      KAFKA_LOG4J_LOGGERS: \"kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO\"
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
    networks:
      - kafka-network
    healthcheck:
      test: [\"CMD-SHELL\", \"kafka-topics --bootstrap-server localhost:9092 --list\"]
      interval: 30s
      timeout: 10s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-metamorphosis-ui-kraft
    depends_on:
      - kafka
    ports:
      - \"8080:8080\"
    environment:
      KAFKA_CLUSTERS_0_NAME: local-kraft
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    networks:
      - kafka-network

networks:
  kafka-network:
    driver: bridge
")

;; Lightweight KRaft configuration (single container, faster startup)
(def kraft-simple-docker-compose
  "Simple Kafka KRaft configuration for quick development"
  "version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-metamorphosis-simple
    ports:
      - \"9092:9092\"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@localhost:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:29093'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
    healthcheck:
      test: [\"CMD-SHELL\", \"kafka-topics --bootstrap-server localhost:9092 --list\"]
      interval: 30s
      timeout: 10s
      retries: 5
")

(defn get-compose-config
  "Get docker-compose configuration based on mode.
   
   Modes:
   :zookeeper (default) - Traditional Kafka with Zookeeper
   :kraft - Kafka with KRaft (no Zookeeper) 
   :simple - Simple KRaft setup (single container, faster startup)
   
   Usage:
   (get-compose-config :kraft)
   (get-compose-config :simple)"
  ([mode]
   (case mode
     :zookeeper default-docker-compose
     :kraft kraft-docker-compose
     :simple kraft-simple-docker-compose
     default-docker-compose)))

(defn write-docker-compose!
  "Generate a docker-compose.yml file in the project root.
   
   Usage:
   (write-docker-compose!)                    ; Use default (zookeeper) configuration
   (write-docker-compose! :kraft)             ; Use KRaft mode
   (write-docker-compose! :simple)            ; Use simple KRaft mode
   (write-docker-compose! custom-content)     ; Use custom configuration string"
  ([] (write-docker-compose! :zookeeper))
  ([mode-or-content]
   (let [content (if (keyword? mode-or-content)
                   (get-compose-config mode-or-content)
                   mode-or-content)]
     (spit "docker-compose.yml" content)
     (println "📝 docker-compose.yml file generated!" 
              (when (keyword? mode-or-content) 
                (str "(" (name mode-or-content) " mode)"))))))

(defn kafka-docker-up!
  "Start Kafka and related services using Docker Compose.
   Generates docker-compose.yml if it doesn't exist.
   
   Usage:
   (kafka-docker-up!)                         ; Use default (zookeeper) configuration
   (kafka-docker-up! :kraft)                  ; Use KRaft mode (no Zookeeper)
   (kafka-docker-up! :simple)                 ; Use simple KRaft mode
   (kafka-docker-up! custom-compose-content)  ; Use custom configuration string"
  ([] (kafka-docker-up! :zookeeper))
  ([mode-or-content]
   (when-not (.exists (io/file "docker-compose.yml"))
     (write-docker-compose! mode-or-content))
   
   (let [mode-name (if (keyword? mode-or-content) (name mode-or-content) "custom")]
     (println "🐳 Starting Kafka with Docker Compose (" mode-name "mode)...")
     (let [{:keys [exit err]} (sh "docker-compose" "up" "-d")]
       (if (zero? exit)
         (do
           (println "✅ Kafka services started successfully!")
           (when (not= mode-or-content :simple)
             (println "📊 Kafka UI available at: http://localhost:8080"))
           (println "🔌 Kafka broker available at: localhost:9092")
           (println "📝 Use (kafka-docker-logs!) to see logs")
           (println "🛑 Use (kafka-docker-down!) to stop services"))
         (do
           (println "❌ Error starting Kafka services:")
           (println err)))))))

(defn kafka-docker-down!
  "Stop and remove Kafka services using Docker Compose.
   
   Usage:
   (kafka-docker-down!)           ; Stop services
   (kafka-docker-down! true)      ; Stop services and remove volumes"
  ([] (kafka-docker-down! false))
  ([remove-volumes?]
   (println "🛑 Stopping Kafka services...")
   (let [cmd (if remove-volumes?
               ["docker-compose" "down" "-v"]
               ["docker-compose" "down"])
         {:keys [exit err]} (apply sh cmd)]
     (if (zero? exit)
       (do
         (println "✅ Kafka services stopped successfully!")
         (when remove-volumes?
           (println "🗑️ Volumes removed")))
       (do
         (println "❌ Error stopping Kafka services:")
         (println err))))))

(defn kafka-docker-status
  "Check the status of Kafka Docker services.
   
   Usage:
   (kafka-docker-status)"
  []
  (println "📊 Checking Kafka Docker services status...")
  (let [{:keys [exit out err]} (sh "docker-compose" "ps")]
    (if (zero? exit)
      (println out)
      (do
        (println "❌ Error checking services status:")
        (println err)))))

(defn kafka-docker-logs!
  "Show logs from Kafka Docker services.
   
   Usage:
   (kafka-docker-logs!)               ; Show all logs
   (kafka-docker-logs! \"kafka\")       ; Show logs for specific service
   (kafka-docker-logs! \"kafka\" true)  ; Follow logs for specific service"
  ([] (kafka-docker-logs! nil false))
  ([service] (kafka-docker-logs! service false))
  ([service follow?]
   (let [cmd (cond
               (and service follow?) ["docker-compose" "logs" "-f" service]
               service ["docker-compose" "logs" service]
               follow? ["docker-compose" "logs" "-f"]
               :else ["docker-compose" "logs"])
         {:keys [exit out err]} (apply sh cmd)]
     (if (zero? exit)
       (println out)
       (do
         (println "❌ Error getting logs:")
         (println err))))))

(defn kafka-docker-restart!
  "Restart Kafka Docker services.
   
   Usage:
   (kafka-docker-restart!)            ; Restart all services
   (kafka-docker-restart! \"kafka\")    ; Restart specific service"
  ([] (kafka-docker-restart! nil))
  ([service]
   (println "🔄 Restarting Kafka services...")
   (let [cmd (if service
               ["docker-compose" "restart" service]
               ["docker-compose" "restart"])
         {:keys [exit err]} (apply sh cmd)]
     (if (zero? exit)
       (println "✅ Services restarted successfully!")
       (do
         (println "❌ Error restarting services:")
         (println err))))))

(defn quick-admin
  "Create an admin client for quick testing"
  []
  (admin/create-admin-client local-kafka-config))

(defn setup-dev-topic
  "Create a development topic with sensible defaults"
  [topic-name & [options]]
  (let [a (quick-admin)
        opts (merge {:partitions 3 :replication-factor 1} options)]
    (try
      (admin/create-topic-if-not-exists! a topic-name opts)
      (finally
        (admin/close! a)))))

(defn- wait-for-kafka-helper
  [start-time timeout-ms]
  (try
    (let [admin-client (quick-admin)]
      (admin/list-topics admin-client)
      (admin/close! admin-client)
      (println "✅ Kafka is ready!")
      true)
    (catch Exception _
      (let [elapsed (- (System/currentTimeMillis) start-time)]
        (if (< elapsed timeout-ms)
          (do
            (Thread/sleep 2000)
            (print ".")
            (flush)
            (wait-for-kafka-helper start-time timeout-ms))
          (do
            (println)
            (println "❌ Timeout waiting for Kafka to be ready")
            false))))))

(defn wait-for-kafka
  "Wait for Kafka to be ready by checking if we can connect.
   Returns true if Kafka is ready, false if timeout."
  ([] (wait-for-kafka 60))
  ([timeout-seconds]
   (println "⏳ Waiting for Kafka to be ready...")
   (wait-for-kafka-helper (System/currentTimeMillis) (* timeout-seconds 1000))))



(defn kafka-dev-setup!
  "Complete development setup: start Kafka, wait for it to be ready, and create common topics.
   
   Usage:
   (kafka-dev-setup!)                                    ; Default: Zookeeper mode with default topics
   (kafka-dev-setup! :kraft)                            ; KRaft mode with default topics
   (kafka-dev-setup! :simple)                           ; Simple KRaft mode with default topics
   (kafka-dev-setup! [\"topic1\" \"topic2\"])               ; Zookeeper mode with custom topics
   (kafka-dev-setup! :kraft [\"topic1\" \"topic2\"])        ; KRaft mode with custom topics"
  ([] (kafka-dev-setup! :zookeeper ["dev-topic" "test-topic" "metamorphosis-topic"]))
  ([mode-or-topics] 
   (if (keyword? mode-or-topics)
     (kafka-dev-setup! mode-or-topics ["dev-topic" "test-topic" "metamorphosis-topic"])
     (kafka-dev-setup! :zookeeper mode-or-topics)))
  ([mode topics]
   (let [mode-name (name mode)]
     (println "🪲 Setting up Kafka development environment (" mode-name "mode)...")
     
     ;; Start Kafka with specified mode
     (kafka-docker-up! mode)
     
     ;; Wait for Kafka to be ready
     (when (wait-for-kafka)
       ;; Create development topics
       (println "📋 Creating development topics...")
       (doseq [topic topics]
         (setup-dev-topic topic {:partitions 3}))
       
       (println "🎉 Development environment ready!")
       (when (not= mode :simple)
         (println "📊 Kafka UI: http://localhost:8080"))
       (println "🔌 Kafka broker: localhost:9092")
       (println "📝 Available topics:" (clojure.string/join ", " topics))
       (println "🏗️ Architecture:" 
                (case mode
                  :zookeeper "Traditional Kafka + Zookeeper"
                  :kraft "KRaft mode (no Zookeeper)"
                  :simple "Simple KRaft (minimal setup)"
                  "Custom configuration"))))))

;; Convenience functions for different modes
(defn kafka-up-zookeeper!
  "Start Kafka with traditional Zookeeper mode.
   
   Usage:
   (kafka-up-zookeeper!)              ; Start with Zookeeper + Kafka UI"
  []
  (kafka-docker-up! :zookeeper))

(defn kafka-up-kraft!
  "Start Kafka with KRaft mode (no Zookeeper).
   Modern Kafka architecture without Zookeeper dependency.
   
   Usage:
   (kafka-up-kraft!)                  ; Start with KRaft + Kafka UI"
  []
  (kafka-docker-up! :kraft))

(defn kafka-up-simple!
  "Start Kafka with simple KRaft mode.
   Minimal setup, fastest startup, no UI.
   
   Usage:
   (kafka-up-simple!)                 ; Start minimal Kafka only"
  []
  (kafka-docker-up! :simple))

(defn kafka-setup-zookeeper!
  "Complete setup with traditional Zookeeper mode.
   
   Usage:
   (kafka-setup-zookeeper!)           ; Setup with default topics
   (kafka-setup-zookeeper! [\"topic1\"]) ; Setup with custom topics"
  ([] (kafka-dev-setup! :zookeeper))
  ([topics] (kafka-dev-setup! :zookeeper topics)))

(defn kafka-setup-kraft!
  "Complete setup with KRaft mode (no Zookeeper).
   Modern Kafka architecture.
   
   Usage:
   (kafka-setup-kraft!)               ; Setup with default topics
   (kafka-setup-kraft! [\"topic1\"])     ; Setup with custom topics"
  ([] (kafka-dev-setup! :kraft))
  ([topics] (kafka-dev-setup! :kraft topics)))

(defn kafka-setup-simple!
  "Complete setup with simple KRaft mode.
   Minimal, fastest setup.
   
   Usage:
   (kafka-setup-simple!)              ; Setup with default topics
   (kafka-setup-simple! [\"topic1\"])    ; Setup with custom topics"
  ([] (kafka-dev-setup! :simple))
  ([topics] (kafka-dev-setup! :simple topics)))

(defn kafka-dev-teardown!
  "Complete teardown: stop all services and optionally remove data.
   
   Usage:
   (kafka-dev-teardown!)              ; Stop services, keep data
   (kafka-dev-teardown! true)         ; Stop services and remove all data"
  ([] (kafka-dev-teardown! false))
  ([remove-volumes?]
   (kafka-docker-down! remove-volumes?)))

(defn quick-producer
  "Create a producer client for quick testing"
  []
  (producer/create string-producer-config))

(defn quick-consumer
  "Create a consumer client for quick testing"
  [group-id]
  (consumer/create (assoc string-consumer-config :group-id group-id)))

(defn list-all-topics
  "List all topics in the local Kafka cluster"
  []
  (let [a (quick-admin)]
    (try
      (let [topics (admin/list-topics a)]
        (println "📋 Topics in cluster:")
        (doseq [topic (sort topics)]
          (println "  -" topic))
        topics)
      (finally
        (admin/close! a)))))

(defn describe-dev-topic
  "Describe a topic with detailed information"
  [topic-name]
  (let [a (quick-admin)]
    (try
      (if (admin/topic-exists? a topic-name)
        (let [info (admin/describe-topic a topic-name)]
          (println "📊 Topic:" topic-name)
          (println "   Partitions:" (count (:partitions info)))
          (println "   Internal:" (:internal? info))
          (doseq [{:keys [partition leader replicas]} (:partitions info)]
            (println "   Partition" partition "- Leader:" (:id leader) "Replicas:" replicas))
          info)
        (println "❌ Topic" topic-name "does not exist"))
      (finally
        (admin/close! a)))))

(defn delete-dev-topic
  "Delete a development topic (use with caution!)"
  [topic-name]
  (let [a (quick-admin)]
    (try
      (if (admin/topic-exists? a topic-name)
        (do
          (print "⚠️  Are you sure you want to delete topic" topic-name "? (y/N): ")
          (flush)
          (let [response (read-line)]
            (if (= (clojure.string/lower-case response) "y")
              (admin/delete-topic! a topic-name)
              (println "❌ Topic deletion cancelled"))))
        (println "ℹ️ Topic" topic-name "does not exist"))
      (finally
        (admin/close! a)))))

(defn cluster-info
  "Get information about the local Kafka cluster"
  []
  (let [a (quick-admin)]
    (try
      (let [info (admin/get-cluster-info a)]
        (println "🏢 Cluster Info:")
        (println "   Cluster ID:" (:cluster-id info))
        (println "   Controller:" (get-in info [:controller :host]) ":" (get-in info [:controller :port]))
        (println "   Nodes:" (count (:nodes info)))
        (doseq [{:keys [id host port]} (:nodes info)]
          (println "     Node" id "-" host ":" port))
        info)
      (finally
        (admin/close! a)))))

(defn send-test-messages
  "Send a few test messages to a topic"
  [topic-name & [num-messages]]
  (let [p (quick-producer)
        messages (or num-messages 5)]
    (println "🪲 Sending" messages "test messages to" topic-name)
    (doseq [i (range messages)]
      (let [key (str "test-key-" i)
            value (str "Test message " i " - " (java.util.Date.))]
        (producer/send! p topic-name key value)
        (println "  Sent:" key)))
    (producer/close! p)
    (println "✅ Done sending test messages")))

(defn read-test-messages
  "Read messages from a topic and print them"
  [topic-name & [group-id]]
  (let [c (quick-consumer (or group-id "dev-reader-group"))]
    (println "🪲 Reading messages from" topic-name)
    (consumer/subscribe! c [topic-name])
    (dotimes [_ 3]
      (let [records (consumer/poll! c 2000)]
        (if (empty? records)
          (println "  No messages in this poll...")
          (doseq [record records]
            (println "  Read:" (:key record) "->" (:value record))))))
    (consumer/close! c)
    (println "✅ Done reading messages")))

(comment
  ;; KAFKA DOCKER SETUP - Choose your architecture:
  
  ;; === OPTION 1: Modern KRaft mode (No Zookeeper) ===
  ;; Fastest, simplest, modern Kafka architecture
  (kafka-setup-kraft!)                    ; Complete setup with KRaft
  (kafka-up-kraft!)                       ; Just start Kafka with KRaft
  
  ;; === OPTION 2: Minimal KRaft (Super fast) ===  
  ;; Single container, fastest startup, no UI
  (kafka-setup-simple!)                   ; Minimal complete setup
  (kafka-up-simple!)                      ; Just start minimal Kafka
  
  ;; === OPTION 3: Traditional Zookeeper mode ===
  ;; Classic Kafka architecture
  (kafka-setup-zookeeper!)                ; Complete setup with Zookeeper
  (kafka-up-zookeeper!)                   ; Just start Kafka with Zookeeper
  
  ;; === GENERIC METHODS ===
  ;; Complete setup with mode selection
  (kafka-dev-setup!)                      ; Default: Zookeeper mode
  (kafka-dev-setup! :kraft)               ; KRaft mode
  (kafka-dev-setup! :simple)              ; Simple KRaft mode
  (kafka-dev-setup! :kraft ["my-topic"])  ; KRaft with custom topics
  
  ;; Manual control
  (kafka-docker-up!)                      ; Default: Zookeeper
  (kafka-docker-up! :kraft)               ; KRaft mode
  (kafka-docker-up! :simple)              ; Simple mode
  
  ;; === MANAGEMENT ===
  ;; Wait for Kafka to be ready
  (wait-for-kafka)
  
  ;; Check Docker services status
  (kafka-docker-status)
  
  ;; View logs
  (kafka-docker-logs!)
  (kafka-docker-logs! "kafka" true)       ; Follow kafka logs
  
  ;; Stop Kafka
  (kafka-docker-down!)
  
  ;; Complete teardown (removes data)
  (kafka-dev-teardown! true)
  
  ;; MANUAL TOPIC MANAGEMENT:
  
  ;; 1. Setup a development topic
  (setup-dev-topic "dev-topic" {:partitions 5})
  
  ;; 2. List all topics
  (list-all-topics)
  
  ;; 3. Describe a topic
  (describe-dev-topic "dev-topic")
  
  ;; 4. Send some test messages
  (send-test-messages "dev-topic" 3)
  
  ;; 5. Read them back
  (read-test-messages "dev-topic")
  
  ;; 6. Get cluster information
  (cluster-info)
  
  ;; 7. Test async sending
  (let [p (quick-producer)]
    (producer/send-async! p "dev-topic" "async-key" "Async test message"
                          (fn [metadata exception]
                            (if exception
                              (println "Error:" (.getMessage exception))
                              (println "Success! Partition:" (.partition metadata)))))
    (producer/close! p))
  
  ;; 8. Clean up topic when done (optional)
  (delete-dev-topic "dev-topic")
  )
