(ns kafka-metamorphosis.consumer
  "Kafka consumer functions with idiomatic Clojure interface"
  (:require [kafka-metamorphosis.util :as util])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords ConsumerRecord]
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
    :enable-auto-commit true}"
  [config]
  (let [normalized-config (util/normalize-config config)
        props (util/map->properties normalized-config)]
    (KafkaConsumer. props)))

(defn subscribe!
  "Subscribe to the given list of topics.
   
   Usage:
   (subscribe! consumer [\"topic1\" \"topic2\"])
   (subscribe! consumer [\"topic1\"])"
  [consumer topics]
  (.subscribe consumer ^Collection topics))

(defn poll!
  "Poll for new records with the given timeout in milliseconds.
   Returns a seq of maps with keys: :topic :partition :offset :key :value :timestamp
   
   Usage:
   (poll! consumer 1000) ; Poll for 1 second"
  [consumer timeout-ms]
  (let [records (.poll consumer (Duration/ofMillis timeout-ms))]
    (map (fn [^ConsumerRecord record]
           {:topic (.topic record)
            :partition (.partition record)
            :offset (.offset record)
            :key (.key record)
            :value (.value record)
            :timestamp (.timestamp record)
            :headers (into {} (map (fn [header]
                                    [(.key header) (.value header)])
                                  (.headers record)))})
         records)))

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
  (let [topic-partitions (map (fn [{:keys [topic partition]}]
                               (TopicPartition. topic partition))
                             partition-maps)]
    (.assign consumer topic-partitions)))

(defn seek!
  "Seek to a specific offset for a given topic partition.
   
   Usage:
   (seek! consumer \"my-topic\" 0 100) ; Seek to offset 100 in partition 0"
  [consumer topic partition offset]
  (let [topic-partition (TopicPartition. topic partition)]
    (.seek consumer topic-partition offset)))

(defn commit-sync!
  "Commit the current consumed offsets synchronously."
  [consumer]
  (.commitSync consumer))

(defn commit-async!
  "Commit the current consumed offsets asynchronously.
   Optional callback function receives a map of topic-partitions to offsets and any exception.
   
   Usage:
   (commit-async! consumer)
   (commit-async! consumer (fn [offsets exception]
                            (when exception
                              (println \"Commit failed:\" (.getMessage exception)))))"
  ([consumer]
   (.commitAsync consumer))
  ([consumer callback-fn]
   (.commitAsync consumer
                 (reify org.apache.kafka.clients.consumer.OffsetCommitCallback
                   (onComplete [_ offsets exception]
                     (callback-fn offsets exception))))))

(defn close!
  "Close the consumer and release resources.
   Optionally specify timeout in milliseconds."
  ([consumer]
   (.close consumer))
  ([consumer timeout-ms]
   (.close consumer (Duration/ofMillis timeout-ms))))
