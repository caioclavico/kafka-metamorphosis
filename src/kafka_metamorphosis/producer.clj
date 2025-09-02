(ns kafka-metamorphosis.producer
  "Kafka producer functions with idiomatic Clojure interface"
  (:require [kafka-metamorphosis.util :as util]
            [kafka-metamorphosis.serializers :as serializers])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord Callback]))

(defn create
  "Create a Kafka producer with the given configuration map.
   
   Example config:
   {:bootstrap-servers \"localhost:9092\"
    :key-serializer \"org.apache.kafka.common.serialization.StringSerializer\"
    :value-serializer \"org.apache.kafka.common.serialization.StringSerializer\"
    :acks \"all\"
    :retries 3
    :schemas true}  ; Enable automatic schema validation based on topic
   
   Schema validation options:
   - :schemas true - Auto-detect schema based on topic name (:topic/default or :topic)
   - :schemas :my-schema - Use specific schema for all messages
   - :schemas {\"topic1\" :schema1 \"topic2\" :schema2} - Per-topic schema mapping"
  [config]
  (let [schema-config (:schemas config)
        producer-config (dissoc config :schemas)
        normalized-config (util/normalize-config producer-config)
        props (util/map->properties normalized-config)
        kafka-producer (KafkaProducer. props)]
    
    ;; Return a map containing the producer and schema config
    {:kafka-producer kafka-producer
     :schema-config schema-config
     :value-serializer (get normalized-config "value.serializer")}))

(defn- auto-serialize-value
  "Automatically serialize value based on the configured serializer.
   If value-serializer is StringSerializer and value is a map, convert to JSON."
  [producer-map value]
  (if (and (map? value)
           (= (:value-serializer producer-map) "org.apache.kafka.common.serialization.StringSerializer"))
    (serializers/to-json value)
    value))

(defn- validate-if-needed
  "Validate message against schema if schema validation is enabled.
   Returns true if validation passes or is disabled, false if validation fails."
  [producer-map topic value]
  (if-let [schema-config (:schema-config producer-map)]
    (try
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
              (if (validate-fn value topic)
                true
                (let [explanation (explain-fn value topic)]
                  (when-not (:valid? explanation)
                    (println (str "⚠️  Schema validation failed for topic '" topic "'"))
                    (println (str "   Message: " value))
                    (println (str "   Errors: " (:errors explanation)))
                    (println "   Message will NOT be sent to Kafka."))
                  false))
              ;; No schema found but validation is required
              (do
                (println (str "⚠️  No schema found for topic '" topic "' but validation is enabled"))
                (println (str "   Message: " value))
                (println "   Message will NOT be sent to Kafka.")
                false)))
          
          ;; Specific schema for all topics
          (keyword? schema-config)
          (let [validate-message-fn (resolve 'kafka-metamorphosis.schema/validate-message)
                explain-validation-fn (resolve 'kafka-metamorphosis.schema/explain-validation)]
            (if (validate-message-fn value schema-config)
              true
              (let [explanation (explain-validation-fn value schema-config)]
                (println (str "⚠️  Schema validation failed for topic '" topic "' using schema '" schema-config "'"))
                (println (str "   Message: " value))
                (println (str "   Errors: " (:errors explanation)))
                (println "   Message will NOT be sent to Kafka.")
                false)))
          
          ;; Per-topic schema mapping
          (map? schema-config)
          (if-let [schema-id (get schema-config topic)]
            (let [validate-message-fn (resolve 'kafka-metamorphosis.schema/validate-message)
                  explain-validation-fn (resolve 'kafka-metamorphosis.schema/explain-validation)]
              (if (validate-message-fn value schema-id)
                true
                (let [explanation (explain-validation-fn value schema-id)]
                  (println (str "⚠️  Schema validation failed for topic '" topic "' using schema '" schema-id "'"))
                  (println (str "   Message: " value))
                  (println (str "   Errors: " (:errors explanation)))
                  (println "   Message will NOT be sent to Kafka.")
                  false)))
            ;; No schema mapping for this topic - reject message
            (do
              (println (str "⚠️  No schema mapping found for topic '" topic "'"))
              (println (str "   Available mappings: " (keys schema-config)))
              (println (str "   Message: " value))
              (println "   Message will NOT be sent to Kafka.")
              false))
          
          :else true))
      (catch java.io.FileNotFoundException _e
        ;; Schema validation not available, continue without validation
        true))
    ;; No schema config, allow sending
    true))

(defn send!
  "Send a message to a Kafka topic synchronously.
   Returns RecordMetadata on success, nil if validation fails.
   
   If the producer was created with :schemas config, automatic validation will be performed.
   
   Usage:
   (send! producer \"my-topic\" \"key\" \"value\")
   (send! producer \"my-topic\" nil \"value-only\") ; key can be nil"
  ([producer topic value]
   (send! producer topic nil value))
  ([producer topic key value]
   ;; Handle both raw KafkaProducer and our producer map
   (if (map? producer)
     ;; Validate if schema validation is enabled
     (if (validate-if-needed producer topic value)
       ;; Send with the actual Kafka producer
       (let [kafka-producer (:kafka-producer producer)
             serialized-value (auto-serialize-value producer value)
             record (ProducerRecord. topic key serialized-value)]
         (.get (.send kafka-producer record)))
       ;; Validation failed, don't send
       nil)
     ;; Legacy support for raw KafkaProducer
     (let [record (ProducerRecord. topic key value)]
       (.get (.send producer record))))))

(defn send-async!
  "Send a message to a Kafka topic asynchronously with optional callback.
   Returns a Future, or nil if validation fails.
   
   If the producer was created with :schemas config, automatic validation will be performed.
   
   Callback function receives [metadata exception] where:
   - metadata: RecordMetadata if successful, nil if error
   - exception: Exception if error occurred, nil if successful
   
   Usage:
   (send-async! producer \"my-topic\" \"key\" \"value\")
   (send-async! producer \"my-topic\" \"key\" \"value\" 
                (fn [metadata exception]
                  (if exception
                    (println \"Error:\" (.getMessage exception))
                    (println \"Sent to partition:\" (.partition metadata)))))"
  ([producer topic value]
   (send-async! producer topic nil value nil))
  ([producer topic key value]
   (send-async! producer topic key value nil))
  ([producer topic key value callback-fn]
   ;; Handle both raw KafkaProducer and our producer map
   (if (map? producer)
     ;; Validate if schema validation is enabled
     (if (validate-if-needed producer topic value)
       ;; Send with the actual Kafka producer
       (let [kafka-producer (:kafka-producer producer)
             serialized-value (auto-serialize-value producer value)
             record (ProducerRecord. topic key serialized-value)
             callback (when callback-fn
                        (reify Callback
                          (onCompletion [_ metadata exception]
                            (callback-fn metadata exception))))]
         (if callback
           (.send kafka-producer record callback)
           (.send kafka-producer record)))
       ;; Validation failed, don't send
       nil)
     ;; Legacy support for raw KafkaProducer
     (let [record (ProducerRecord. topic key value)
           callback (when callback-fn
                      (reify Callback
                        (onCompletion [_ metadata exception]
                          (callback-fn metadata exception))))]
       (if callback
         (.send producer record callback)
         (.send producer record))))))

(defn close!
  "Close the producer and release resources.
   Optionally specify timeout in milliseconds."
  ([producer]
   ;; Handle both raw KafkaProducer and our producer map
   (if (map? producer)
     (.close (:kafka-producer producer))
     (.close producer)))
  ([producer timeout-ms]
   ;; Handle both raw KafkaProducer and our producer map
   (if (map? producer)
     (.close (:kafka-producer producer) (java.time.Duration/ofMillis timeout-ms))
     (.close producer (java.time.Duration/ofMillis timeout-ms)))))

(defn flush!
  "Flush any buffered records to the Kafka cluster.
   This method makes all buffered records immediately available to send."
  [producer]
  ;; Handle both raw KafkaProducer and our producer map
  (if (map? producer)
    (.flush (:kafka-producer producer))
    (.flush producer)))
