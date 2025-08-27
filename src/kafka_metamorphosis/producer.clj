(ns kafka-metamorphosis.producer
  "Kafka producer functions with idiomatic Clojure interface"
  (:require [kafka-metamorphosis.util :as util])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord Callback]))

(defn create
  "Create a Kafka producer with the given configuration map.
   
   Example config:
   {:bootstrap-servers \"localhost:9092\"
    :key-serializer \"org.apache.kafka.common.serialization.StringSerializer\"
    :value-serializer \"org.apache.kafka.common.serialization.StringSerializer\"
    :acks \"all\"
    :retries 3}"
  [config]
  (let [normalized-config (util/normalize-config config)
        props (util/map->properties normalized-config)]
    (KafkaProducer. props)))

(defn send!
  "Send a message to a Kafka topic synchronously.
   Returns RecordMetadata on success.
   
   Usage:
   (send! producer \"my-topic\" \"key\" \"value\")
   (send! producer \"my-topic\" nil \"value-only\") ; key can be nil"
  ([producer topic value]
   (send! producer topic nil value))
  ([producer topic key value]
   (let [record (ProducerRecord. topic key value)]
     (.get (.send producer record)))))

(defn send-async!
  "Send a message to a Kafka topic asynchronously with optional callback.
   Returns a Future.
   
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
   (let [record (ProducerRecord. topic key value)
         callback (when callback-fn
                    (reify Callback
                      (onCompletion [_ metadata exception]
                        (callback-fn metadata exception))))]
     (if callback
       (.send producer record callback)
       (.send producer record)))))

(defn close!
  "Close the producer and release resources.
   Optionally specify timeout in milliseconds."
  ([producer]
   (.close producer))
  ([producer timeout-ms]
   (.close producer (java.time.Duration/ofMillis timeout-ms))))

(defn flush!
  "Flush any buffered records to the Kafka cluster.
   This method makes all buffered records immediately available to send."
  [producer]
  (.flush producer))
