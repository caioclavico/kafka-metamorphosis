(ns kafka-metamorphosis.consumer
  "Kafka consumer functions with idiomatic Clojure interface"
  (:require [kafka-metamorphosis.util :as util]
            [kafka-metamorphosis.serializers :as serializers])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecord]
           [org.apache.kafka.common TopicPartition]
           [java.time Duration]
           [java.util Collection]))

(defn create
  "Create a Kafka consumer with the given configuration map.
   
   Example config:
   {:bootstrap-servers \"localhost:9092\"
    :group-id \"my-consumer-group\"
    :key-deserializer \"org.apache.kafka.common.serialization.StringDeserializer\"
    :value-deserializer \"org.apache.kafka.common.serialization.StringDeserializer\"
    :auto-offset-reset \"earliest\"
    :enable-auto-commit true
    :schemas true}  ; Enable automatic schema validation based on topic
   
   Schema validation options:
   - :schemas true - Auto-detect schema based on topic name (:topic/default or :topic)
   - :schemas :my-schema - Use specific schema for all messages
   - :schemas {\"topic1\" :schema1 \"topic2\" :schema2} - Per-topic schema mapping"
  [config]
  (let [schema-config (:schemas config)
        consumer-config (dissoc config :schemas)
        normalized-config (util/normalize-config consumer-config)
        props (util/map->properties normalized-config)
        kafka-consumer (KafkaConsumer. props)]
    
    ;; Return a map containing the consumer and schema config
    {:kafka-consumer kafka-consumer
     :schema-config schema-config
     :value-deserializer (get normalized-config "value.deserializer")}))

(defn- auto-parse-value
  "Automatically parse value based on the configured deserializer.
   If value-deserializer is StringDeserializer and value looks like JSON, parse it."
  [consumer-map value]
  (if (and (string? value)
           (= (:value-deserializer consumer-map) "org.apache.kafka.common.serialization.StringDeserializer"))
    (try
      (serializers/from-json value)
      (catch Exception _e
        ;; If JSON parsing fails, return original value
        value))
    value))

(defn- validate-consumed-message
  "Validate consumed message against schema if schema validation is enabled.
   Returns true if validation passes or is disabled, false if validation fails."
  [consumer-map topic value]
  (if-let [schema-config (:schema-config consumer-map)]
    (try
      ;; Parse value if needed for validation
      (let [parsed-value (auto-parse-value consumer-map value)]
        ;; Load schema validation functions dynamically to avoid circular dependency
        (require 'kafka-metamorphosis.schema)
        (let [validate-fn (resolve 'kafka-metamorphosis.schema/validate-message-for-topic)
              explain-fn (resolve 'kafka-metamorphosis.schema/explain-validation-for-topic)]
          (cond
            ;; Auto-detect schema based on topic name
            (= schema-config true)
            (let [get-schema-fn (resolve 'kafka-metamorphosis.schema/get-schema-for-topic)]
              (if (get-schema-fn topic)
                ;; Schema exists, validate
                (if (validate-fn parsed-value topic)
                  true
                  (let [explanation (explain-fn parsed-value topic)]
                  (when-not (:valid? explanation)
                    (println (str "⚠️  Schema validation failed for consumed message from topic '" topic "'"))
                    (println (str "   Message: " value))
                    (println (str "   Errors: " (:errors explanation)))
                    (println "   Message will NOT be processed."))
                  false))
              ;; No schema found but validation is required
              (do
                (println (str "⚠️  No schema found for topic '" topic "' but validation is enabled"))
                (println (str "   Message: " value))
                (println "   Message will NOT be processed.")
                false)))
          
          ;; Specific schema for all topics
          (keyword? schema-config)
          (let [validate-message-fn (resolve 'kafka-metamorphosis.schema/validate-message)
                explain-validation-fn (resolve 'kafka-metamorphosis.schema/explain-validation)]
            (if (validate-message-fn parsed-value schema-config)
              true
              (let [explanation (explain-validation-fn parsed-value schema-config)]
                (println (str "⚠️  Schema validation failed for consumed message from topic '" topic "' using schema '" schema-config "'"))
                (println (str "   Message: " value))
                (println (str "   Errors: " (:errors explanation)))
                (println "   Message will NOT be processed.")
                false)))
          
          ;; Per-topic schema mapping
          (map? schema-config)
          (if-let [schema-id (get schema-config topic)]
            (let [validate-message-fn (resolve 'kafka-metamorphosis.schema/validate-message)
                  explain-validation-fn (resolve 'kafka-metamorphosis.schema/explain-validation)]
              (if (validate-message-fn parsed-value schema-id)
                true
                (let [explanation (explain-validation-fn parsed-value schema-id)]
                  (println (str "⚠️  Schema validation failed for consumed message from topic '" topic "' using schema '" schema-id "'"))
                  (println (str "   Message: " value))
                  (println (str "   Errors: " (:errors explanation)))
                  (println "   Message will NOT be processed.")
                  false)))
            ;; No schema mapping for this topic - reject message
            (do
              (println (str "⚠️  No schema mapping found for topic '" topic "'"))
              (println (str "   Available mappings: " (keys schema-config)))
              (println (str "   Message: " value))
              (println "   Message will NOT be processed.")
              false))
          
          :else true)))
      (catch java.io.FileNotFoundException _e
        ;; Schema validation not available, continue without validation
        true))
    ;; No schema config, allow processing
    true))

(defn subscribe!
  "Subscribe to the given list of topics.
   
   Usage:
   (subscribe! consumer [\"topic1\" \"topic2\"])
   (subscribe! consumer [\"topic1\"])"
  [consumer topics]
  ;; Handle both raw KafkaConsumer and our consumer map
  (if (map? consumer)
    (.subscribe (:kafka-consumer consumer) ^Collection topics)
    (.subscribe consumer ^Collection topics)))

(defn poll!
  "Poll for new records with the given timeout in milliseconds.
   Returns a seq of maps with keys: :topic :partition :offset :key :value :timestamp
   
   If the consumer was created with :schemas config, automatic validation will be performed
   and invalid messages will be filtered out.
   
   Usage:
   (poll! consumer 1000) ; Poll for 1 second"
  [consumer timeout-ms]
  ;; Handle both raw KafkaConsumer and our consumer map
  (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)
        records (.poll kafka-consumer (Duration/ofMillis timeout-ms))]
    (->> records
         (map (fn [^ConsumerRecord record]
                (let [record-map {:topic (.topic record)
                                 :partition (.partition record)
                                 :offset (.offset record)
                                 :key (.key record)
                                 :value (.value record)
                                 :timestamp (.timestamp record)
                                 :headers (into {} (map (fn [header]
                                                         [(.key header) (.value header)])
                                                       (.headers record)))}]
                  ;; Check validation if consumer is our map with schema config
                  (if (map? consumer)
                    (when (validate-consumed-message consumer (.topic record) (.value record))
                      record-map)
                    record-map))))
         (filter some?))))

(defn consume!
  "Consume records continuously with a handler function.
   The handler function receives each record map.
   
   Options:
   :poll-timeout - timeout for each poll in milliseconds (default: 1000)
   :stop-fn - function that returns true when consumption should stop (default: never stop)
   
   Usage:
   (consume! consumer 
             {:poll-timeout 1000
              :handler (fn [record] 
                        (println \"Received:\" (:value record)))
              :stop-fn #(some-condition?)})"
  [consumer {:keys [poll-timeout handler stop-fn] 
             :or {poll-timeout 1000 
                  stop-fn (constantly false)}}]
  (loop []
    (when-not (stop-fn)
      (let [records (poll! consumer poll-timeout)]
        (doseq [record records]
          (handler record))
        (recur)))))

(defn assign!
  "Manually assign the consumer to specific topic partitions.
   
   Usage:
   (assign! consumer [{:topic \"my-topic\" :partition 0}
                      {:topic \"my-topic\" :partition 1}])"
  [consumer partition-maps]
  (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)
        topic-partitions (map (fn [{:keys [topic partition]}]
                               (TopicPartition. topic partition))
                             partition-maps)]
    (.assign kafka-consumer topic-partitions)))

(defn seek!
  "Seek to a specific offset for a given topic partition.
   
   Usage:
   (seek! consumer \"my-topic\" 0 100) ; Seek to offset 100 in partition 0"
  [consumer topic partition offset]
  (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)
        topic-partition (TopicPartition. topic partition)]
    (.seek kafka-consumer topic-partition offset)))

(defn commit-sync!
  "Commit the current consumed offsets synchronously."
  [consumer]
  (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)]
    (.commitSync kafka-consumer)))

(defn commit-async!
  "Commit the current consumed offsets asynchronously.
   Optional callback function receives a map of topic-partitions to offsets and any exception.
   
   Usage:
   (commit-async! consumer)
   (commit-async! consumer (fn [offsets exception]
                            (when exception
                              (println \"Commit failed:\" (.getMessage exception)))))"
  ([consumer]
   (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)]
     (.commitAsync kafka-consumer)))
  ([consumer callback-fn]
   (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)]
     (.commitAsync kafka-consumer
                   (reify org.apache.kafka.clients.consumer.OffsetCommitCallback
                     (onComplete [_ offsets exception]
                       (callback-fn offsets exception)))))))

(defn close!
  "Close the consumer and release resources.
   Optionally specify timeout in milliseconds."
  ([consumer]
   (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)]
     (.close kafka-consumer)))
  ([consumer timeout-ms]
   (let [kafka-consumer (if (map? consumer) (:kafka-consumer consumer) consumer)]
     (.close kafka-consumer (Duration/ofMillis timeout-ms)))))
