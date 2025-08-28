(ns kafka-metamorphosis.core
  "Kafka Metamorphosis - A Clojure wrapper for Apache Kafka
   
   'When Gregor Samsa woke up one morning from unsettling dreams, 
    he found himself changed in his bed into a monstrous vermin.'
   — Franz Kafka, The Metamorphosis
   
   Just as Gregor Samsa underwent his transformation, this library 
   metamorphoses the complex Java Kafka APIs into elegant Clojure functions."
  (:gen-class)
  (:require [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.admin :as admin]
            [kafka-metamorphosis.serializers :as serializers]
            [clojure.string])
  (:gen-class))

;; ============================================================================
;; Configuration Presets
;; ============================================================================

(def default-bootstrap-servers
  "Default Kafka broker address for local development"
  "localhost:9092")

(defn local-config
  "Create a base configuration for local Kafka development.
   
   Usage:
   (local-config)                             ; Use default localhost:9092
   (local-config \"kafka:9092\")              ; Custom broker address"
  ([] (local-config default-bootstrap-servers))
  ([bootstrap-servers]
   {:bootstrap-servers bootstrap-servers}))

(defn producer-config
  "Create a producer configuration with sensible defaults.
   
   Usage:
   (producer-config)                                    ; String serializers, local broker
   (producer-config {:acks \"1\"})                      ; Override specific settings
   (producer-config \"kafka:9092\" {:acks \"all\"})     ; Custom broker + settings
   (producer-config {} string-serializers)              ; Custom serializers
   (producer-config \"kafka:9092\" {} json-serializers) ; JSON serializers"
  ([] 
   (producer-config {} serializers/string-serializers))
  ([overrides]
   (producer-config default-bootstrap-servers overrides serializers/string-serializers))
  ([bootstrap-servers overrides]
   (producer-config bootstrap-servers overrides serializers/string-serializers))
  ([bootstrap-servers overrides serializers]
   (merge (local-config bootstrap-servers)
          serializers
          {:acks "all"
           :retries 3
           :batch-size 16384
           :linger-ms 1
           :buffer-memory 33554432}
          overrides)))

(defn json-producer-config
  "Create a producer configuration for JSON values with sensible defaults.
   Uses simple JSON mode (no schema registry required).
   
   Usage:
   (json-producer-config)                                ; String keys, JSON values, local broker
   (json-producer-config {:acks \"1\"})                  ; Override specific settings
   (json-producer-config \"kafka:9092\" {:acks \"all\"}) ; Custom broker + settings"
  ([] 
   (producer-config {} serializers/simple-json-serializers))
  ([overrides]
   (producer-config default-bootstrap-servers overrides serializers/simple-json-serializers))
  ([bootstrap-servers overrides]
   (producer-config bootstrap-servers overrides serializers/simple-json-serializers)))

(defn consumer-config
  "Create a consumer configuration with sensible defaults.
   
   Usage:
   (consumer-config \"my-group\")               ; String deserializers, local broker
   (consumer-config \"my-group\" {:auto-offset-reset \"latest\"}) ; Override settings
   (consumer-config \"kafka:9092\" \"my-group\" {}) ; Custom broker + group
   (consumer-config \"my-group\" {} string-deserializers) ; Custom deserializers
   (consumer-config \"kafka:9092\" \"my-group\" {} json-deserializers) ; JSON deserializers"
  ([group-id]
   (consumer-config default-bootstrap-servers group-id {} serializers/string-deserializers))
  ([group-id overrides]
   (consumer-config default-bootstrap-servers group-id overrides serializers/string-deserializers))
  ([bootstrap-servers group-id overrides]
   (consumer-config bootstrap-servers group-id overrides serializers/string-deserializers))
  ([bootstrap-servers group-id overrides deserializers]
   (merge (local-config bootstrap-servers)
          deserializers
          {:group-id group-id
           :auto-offset-reset "earliest"
           :enable-auto-commit true
           :auto-commit-interval-ms 1000
           :session-timeout-ms 30000
           :heartbeat-interval-ms 3000}
          overrides)))

(defn json-consumer-config
  "Create a consumer configuration for JSON values with sensible defaults.
   Uses simple JSON mode (no schema registry required).
   
   Usage:
   (json-consumer-config \"my-group\")          ; String keys, JSON values, local broker
   (json-consumer-config \"my-group\" {:auto-offset-reset \"latest\"}) ; Override settings
   (json-consumer-config \"kafka:9092\" \"my-group\" {}) ; Custom broker + group"
  ([group-id]
   (consumer-config default-bootstrap-servers group-id {} serializers/simple-json-deserializers))
  ([group-id overrides]
   (consumer-config default-bootstrap-servers group-id overrides serializers/simple-json-deserializers))
  ([bootstrap-servers group-id overrides]
   (consumer-config bootstrap-servers group-id overrides serializers/simple-json-deserializers)))

(defn admin-config
  "Create an admin client configuration.
   
   Usage:
   (admin-config)                             ; Local broker
   (admin-config \"kafka:9092\")              ; Custom broker"
  ([] (admin-config default-bootstrap-servers))
  ([bootstrap-servers]
   (local-config bootstrap-servers)))

;; ============================================================================
;; Generic Configuration Builder
;; ============================================================================

(defn build-config
  "Generic configuration builder that can be used for producers, consumers, and admin clients.
   
   Usage:
   (build-config {:group-id \"my-group\"} string-deserializers)             ; Consumer config
   (build-config {:acks \"all\"} json-serializers)                          ; Producer config  
   (build-config {} {})                                                     ; Admin config
   (build-config \"kafka:9092\" {:group-id \"group\"} string-deserializers) ; Custom broker"
  ([overrides serializers-or-deserializers]
   (build-config default-bootstrap-servers overrides serializers-or-deserializers))
  ([bootstrap-servers overrides serializers-or-deserializers]
   (merge (local-config bootstrap-servers)
          serializers-or-deserializers
          overrides)))

;; ============================================================================
;; High-Level API Functions
;; ============================================================================

(defn send-message!
  "Send a message to a topic with minimal setup.
   
   Usage:
   (send-message! \"my-topic\" \"Hello, World!\")
   (send-message! \"my-topic\" \"key\" \"Hello, World!\")
   (send-message! \"my-topic\" \"key\" \"Hello, World!\" {:acks \"1\"})"
  ([topic value]
   (send-message! topic nil value {}))
  ([topic key value]
   (send-message! topic key value {}))
  ([topic key value producer-opts]
   (let [p (producer/create (producer-config producer-opts))]
     (try
       (producer/send! p topic key value)
       (finally
         (producer/close! p))))))

(defn send-json-message!
  "Send a JSON message to a topic with minimal setup.
   The value will be automatically converted to JSON.
   
   Usage:
   (send-json-message! \"my-topic\" {:name \"John\" :age 30})
   (send-json-message! \"my-topic\" \"user-123\" {:name \"John\" :age 30})
   (send-json-message! \"my-topic\" \"user-123\" {:name \"John\"} {:acks \"1\"})"
  ([topic value]
   (send-json-message! topic nil value {}))
  ([topic key value]
   (send-json-message! topic key value {}))
  ([topic key value producer-opts]
   (let [p (producer/create (json-producer-config producer-opts))
         json-value (serializers/to-json value)]
     (try
       (producer/send! p topic key json-value)
       (finally
         (producer/close! p))))))

(defn consume-messages!
  "Consume messages from topics with minimal setup.
   Returns a lazy sequence of messages.
   
   Usage:
   (consume-messages \"my-group\" [\"topic1\" \"topic2\"])
   (consume-messages \"my-group\" [\"topic1\"] {:max-messages 10})
   (consume-messages \"my-group\" [\"topic1\"] {:timeout-ms 5000 :max-messages 100})"
  ([group-id topics]
   (consume-messages! group-id topics {}))
  ([group-id topics opts]
   (let [{:keys [timeout-ms max-messages consumer-opts]
          :or {timeout-ms 1000 max-messages Integer/MAX_VALUE}} opts
         c (consumer/create (consumer-config group-id consumer-opts))]
     (consumer/subscribe! c topics)
     (letfn [(poll-messages [remaining]
               (lazy-seq
                 (when (pos? remaining)
                   (let [records (consumer/poll! c timeout-ms)]
                     (if (seq records)
                       (concat records (poll-messages (- remaining (count records))))
                       (poll-messages remaining))))))]
       (poll-messages max-messages)))))

(defn consume-json-messages!
  "Consume JSON messages from topics with minimal setup.
   Returns a lazy sequence of messages with values automatically parsed from JSON.
   
   Usage:
   (consume-json-messages \"my-group\" [\"topic1\" \"topic2\"])
   (consume-json-messages \"my-group\" [\"topic1\"] {:max-messages 10})
   (consume-json-messages \"my-group\" [\"topic1\"] {:timeout-ms 5000 :max-messages 100})"
  ([group-id topics]
   (consume-json-messages! group-id topics {}))
  ([group-id topics opts]
   (let [{:keys [timeout-ms max-messages consumer-opts]
          :or {timeout-ms 1000 max-messages Integer/MAX_VALUE}} opts
         c (consumer/create (json-consumer-config group-id consumer-opts))]
     (consumer/subscribe! c topics)
     (letfn [(poll-messages [remaining]
               (lazy-seq
                 (when (pos? remaining)
                   (let [records (consumer/poll! c timeout-ms)
                         json-records (map #(update % :value serializers/from-json) records)]
                     (if (seq json-records)
                       (concat json-records (poll-messages (- remaining (count json-records))))
                       (poll-messages remaining))))))]
       (poll-messages max-messages)))))

;; ============================================================================
;; Development Helpers
;; ============================================================================

(defn health-check
  "Check if Kafka is accessible and return cluster information.
   
   Usage:
   (health-check)
   (health-check \"kafka:9092\")"
  ([] (health-check default-bootstrap-servers))
  ([bootstrap-servers]
   (try
     (let [a (admin/create-admin-client (admin-config bootstrap-servers))]
       (try
         (let [cluster-info (admin/get-cluster-info a)
               topics (admin/list-topics a)]
           {:status :healthy
            :bootstrap-servers bootstrap-servers
            :cluster-id (:cluster-id cluster-info)
            :nodes (count (:nodes cluster-info))
            :topics (count topics)
            :available-topics (sort topics)})
         (finally
           (admin/close! a))))
     (catch Exception e
       {:status :unhealthy
        :bootstrap-servers bootstrap-servers
        :error (.getMessage e)}))))

;; ============================================================================
;; Main Function
;; ============================================================================

(defn -main
  "Entry point for the Kafka Metamorphosis library.
   Supports different commands and demonstrates library usage."
  [& args]
  (case (first args)
    "health" 
    (let [broker (or (second args) default-bootstrap-servers)
          health (health-check broker)]
      (println "🏥 Kafka Health Check")
      (println "Broker:" (:bootstrap-servers health))
      (if (= (:status health) :healthy)
        (do
          (println "Status: ✅ Healthy")
          (println "Cluster ID:" (:cluster-id health))
          (println "Nodes:" (:nodes health))
          (println "Topics:" (:topics health))
          (when (seq (:available-topics health))
            (println "Available topics:" (clojure.string/join ", " (take 10 (:available-topics health))))
            (when (> (count (:available-topics health)) 10)
              (println "... and" (- (count (:available-topics health)) 10) "more"))))
        (do
          (println "Status: ❌ Unhealthy")
          (println "Error:" (:error health)))))
    
    "topics"
    (let [broker (or (second args) default-bootstrap-servers)]
      (try
        (let [topics (admin/list-topics broker)]
          (println "📋 Topics in cluster" broker ":")
          (if (seq topics)
            (doseq [topic (sort topics)]
              (println "  -" topic))
            (println "  No topics found")))
        (catch Exception e
          (println "❌ Error listing topics:" (.getMessage e)))))
    
    "send"
    (let [[_ topic key value] args]
      (if (and topic value)
        (do
          (println "📤 Sending message to topic:" topic)
          (send-message! topic (or key "kafka-metamorphosis") value)
          (println "✅ Message sent successfully"))
        (println "Usage: lein run send <topic> [key] <value>")))
    
    "send-json"
    (let [[_ topic key value] args]
      (if (and topic value)
        (do
          (println "📤 Sending JSON message to topic:" topic)
          (let [json-value (try
                             (serializers/from-json value)
                             (catch Exception _
                               (println "⚠️  Value is not valid JSON, sending as string")
                               value))]
            (send-json-message! topic (or key "kafka-metamorphosis") json-value))
          (println "✅ JSON message sent successfully"))
        (println "Usage: lein run send-json <topic> [key] <json-value>")))
    
    ;; Default: show help and examples
    (do
      (println "🪲 Kafka Metamorphosis - Transforming Kafka APIs into Clojure elegance")
      (println)
      (println "Usage:")
      (println "  lein run health [broker]      - Check Kafka health")
      (println "  lein run topics [broker]      - List all topics")
      (println "  lein run send <topic> [key] <value> - Send a string message")
      (println "  lein run send-json <topic> [key] <json> - Send a JSON message")
      (println)
      (println "Quick Start Examples:")
      (println)
      (println "🚀 Producer:")
      (println "(require '[kafka-metamorphosis.core :as km])")
      (println "(def p (km/create-producer (km/producer-config)))")
      (println "(km/producer/send! p \"my-topic\" \"key\" \"Hello, Kafka!\")")
      (println "(km/producer/close! p)")
      (println)
      (println "📨 High-level send:")
      (println "(km/send-message! \"my-topic\" \"Hello, World!\")")
      (println)
      (println "🔧 JSON messages:")
      (println "(km/send-json-message! \"my-topic\" {:name \"John\" :age 30})")
      (println "(take 10 (km/consume-json-messages \"my-group\" [\"my-topic\"]))")
      (println)
      (println "🗂️ Custom serializers:")
      (println "(km/create-producer (km/producer-config {} (km.serializers/avro-serializers \"http://localhost:8081\")))")
      (println "(km/create-consumer (km/consumer-config \"my-group\" {} (km.serializers/protobuf-deserializers \"http://localhost:8081\")))")
      (println)
      (println "🎯 Consumer:")
      (println "(def c (km/create-consumer (km/consumer-config \"my-group\")))")
      (println "(km/consumer/subscribe! c [\"my-topic\"])")
      (println "(km/consumer/poll! c 1000)")
      (println "(km/consumer/close! c)")
      (println)
      (println "🔄 High-level consume:")
      (println "(take 10 (km/consume-messages \"my-group\" [\"my-topic\"]))")
      (println)
      (println "🏗️ Admin operations:")
      (println "(km/create-topic! \"new-topic\" {:partitions 6})")
      (println "(km/list-topics)")
      (println "(km/topic-exists? \"my-topic\")")
      (println)
      (println "🏥 Health check:")
      (println "(km/health-check)")
      (println)
      (println "The metamorphosis is complete! 🦋"))))
