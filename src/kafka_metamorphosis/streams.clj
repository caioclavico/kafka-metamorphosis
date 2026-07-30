(ns kafka-metamorphosis.streams
  "Kafka Streams wrapper for Kafka Metamorphosis."
  (:require [kafka-metamorphosis.util :as util])
  (:import [org.apache.kafka.streams KafkaStreams StreamsBuilder StoreQueryParameters Topology KeyValue]
           [org.apache.kafka.streams.kstream KStream KTable GlobalKTable KGroupedStream
                                              TimeWindowedKStream TimeWindows Materialized
                                              ValueMapper Predicate KeyValueMapper ValueJoiner
                                              Reducer Aggregator Initializer]
           [org.apache.kafka.streams.state QueryableStoreTypes]
           [java.time Duration]
           [java.util ArrayList]))

(def default-application-id
  "Default Kafka Streams application id."
  "kafka-metamorphosis-streams-app")

(defn streams-config
  "Create a Kafka Streams configuration map with sensible defaults.

   Usage:
   (streams-config)                             ; local brokers and default application id
   (streams-config \"my-app\")                  ; custom application id
   (streams-config \"my-app\" {:bootstrap-servers \"kafka:9092\"})"
  ([]
   (streams-config default-application-id {}))
  ([application-id]
   (streams-config application-id {}))
  ([application-id overrides]
   (merge {:application-id application-id
           :bootstrap-servers "localhost:9092"
           :default-key-serde "org.apache.kafka.common.serialization.Serdes$StringSerde"
           :default-value-serde "org.apache.kafka.common.serialization.Serdes$StringSerde"
           :processing-guarantee "at_least_once"
           :commit-interval-ms 1000
           :cache-max-bytes-buffering 10485760
           :auto-offset-reset "earliest"}
          overrides)))

(defn create-builder
  "Create a new Kafka Streams builder."
  []
  (StreamsBuilder.))

(defn stream
  "Create a KStream from a builder and a topic or list of topics." 
  [^StreamsBuilder builder topics]
  (if (string? topics)
    (.stream builder topics)
    (.stream builder (ArrayList. (into [] topics)))))

(defn to
  "Send a KStream to the given topic and return the same stream."
  [^KStream stream topic]
  (.to stream topic)
  stream)

(defn map-values
  "Transform the values of a KStream using f.

   f receives the current value and should return the transformed value." 
  [^KStream stream f]
  (.mapValues stream
              (reify ValueMapper
                (apply [_ value]
                  (f value)))))

(defn filter-stream
  "Filter records from a KStream using a predicate function of [key value]." 
  [^KStream stream pred]
  (.filter stream
           (reify Predicate
             (test [_ key value]
               (boolean (pred key value))))))

(defn branch
  "Split a KStream into multiple streams using a sequence of predicate functions.
   Returns a vector of KStreams in the same order as predicates." 
  [^KStream stream predicates]
  (let [pred-array (into-array Predicate
                               (map (fn [pred]
                                      (reify Predicate
                                        (test [_ key value]
                                          (boolean (pred key value)))))
                                    predicates))]
    (vec (.branch stream pred-array))))

(defn build-topology
  "Build a Topology from a StreamsBuilder."
  [^StreamsBuilder builder]
  (.build builder))

;; =============================================================================
;; Tables
;; =============================================================================

(defn table
  "Create a KTable from a builder and a topic.
   Pass a :store name to make the underlying state store queryable via `query`."
  ([^StreamsBuilder builder topic]
   (.table builder ^String topic))
  ([^StreamsBuilder builder topic {:keys [store]}]
   (if store
     (.table builder ^String topic (Materialized/as ^String store))
     (.table builder ^String topic))))

(defn global-table
  "Create a GlobalKTable from a builder and a topic.
   Pass a :store name to make the underlying state store queryable via `query`."
  ([^StreamsBuilder builder topic]
   (.globalTable builder ^String topic))
  ([^StreamsBuilder builder topic {:keys [store]}]
   (if store
     (.globalTable builder ^String topic (Materialized/as ^String store))
     (.globalTable builder ^String topic))))

;; =============================================================================
;; Transformations
;; =============================================================================

(defn map-kv
  "Transform both key and value of a KStream using f, a function of [key value]
   that returns a [new-key new-value] pair." 
  [^KStream stream f]
  (.map stream
        (reify KeyValueMapper
          (apply [_ key value]
            (let [[k v] (f key value)]
              (KeyValue. k v))))))

(defn flat-map-kv
  "Transform each [key value] pair of a KStream into zero or more [key value] pairs
   using f, a function of [key value] that returns a sequence of pairs."
  [^KStream stream f]
  (.flatMap stream
            (reify KeyValueMapper
              (apply [_ key value]
                (mapv (fn [[k v]] (KeyValue. k v)) (f key value))))))

;; =============================================================================
;; Grouping and windowing
;; =============================================================================

(defn group
  "Group a KStream by its existing key, returning a KGroupedStream."
  [^KStream stream]
  (.groupByKey stream))

(defn group-by-key
  "Group a KStream by a new key computed from [key value] using f,
   returning a KGroupedStream."
  [^KStream stream f]
  (.groupBy stream
            (reify KeyValueMapper
              (apply [_ key value]
                (f key value)))))

(defn window
  "Apply a tumbling time window of the given size (in milliseconds) to a
   KGroupedStream, returning a TimeWindowedKStream."
  [^KGroupedStream grouped-stream size-ms]
  (.windowedBy grouped-stream (TimeWindows/ofSizeWithNoGrace (Duration/ofMillis size-ms))))

;; =============================================================================
;; Aggregations (accept a KGroupedStream or a TimeWindowedKStream)
;; =============================================================================

(defn count-values
  "Count records per key in a grouped (or windowed) stream, returning a KTable."
  ([grouped-stream]
   (.count grouped-stream))
  ([grouped-stream store-name]
   (.count grouped-stream (Materialized/as ^String store-name))))

(defn reduce-values
  "Reduce values per key in a grouped (or windowed) stream using f,
   a function of [aggregate value] -> new-aggregate. Returns a KTable."
  ([grouped-stream f]
   (.reduce grouped-stream
            (reify Reducer
              (apply [_ agg value]
                (f agg value)))))
  ([grouped-stream f store-name]
   (.reduce grouped-stream
            (reify Reducer
              (apply [_ agg value]
                (f agg value)))
            (Materialized/as ^String store-name))))

(defn aggregate
  "Aggregate values per key in a grouped (or windowed) stream.
   init-fn is a no-arg function producing the initial aggregate value.
   agg-fn is a function of [key value aggregate] -> new-aggregate.
   Returns a KTable."
  ([grouped-stream init-fn agg-fn]
   (.aggregate grouped-stream
               (reify Initializer
                 (apply [_] (init-fn)))
               (reify Aggregator
                 (apply [_ key value agg] (agg-fn key value agg)))))
  ([grouped-stream init-fn agg-fn store-name]
   (.aggregate grouped-stream
               (reify Initializer
                 (apply [_] (init-fn)))
               (reify Aggregator
                 (apply [_ key value agg] (agg-fn key value agg)))
               (Materialized/as ^String store-name))))

;; =============================================================================
;; Joins
;; =============================================================================

(defn join
  "Inner join a KStream with a KTable using value-joiner-fn,
   a function of [stream-value table-value] -> joined-value."
  [^KStream stream ^KTable ktable value-joiner-fn]
  (.join stream ktable
         (reify ValueJoiner
           (apply [_ v1 v2]
             (value-joiner-fn v1 v2)))))

(defn left-join
  "Left join a KStream with a KTable using value-joiner-fn,
   a function of [stream-value table-value] -> joined-value.
   table-value is nil when there is no match."
  [^KStream stream ^KTable ktable value-joiner-fn]
  (.leftJoin stream ktable
             (reify ValueJoiner
               (apply [_ v1 v2]
                 (value-joiner-fn v1 v2)))))

(defn create
  "Create a KafkaStreams instance from an existing Topology and config map."
  [^Topology topology config]
  (let [normalized-config (util/normalize-config config)
        props (util/map->properties normalized-config)]
    (KafkaStreams. topology props)))

(defn create-from-builder
  "Create a KafkaStreams instance from a StreamsBuilder and config map."
  [^StreamsBuilder builder config]
  (create (build-topology builder) config))

(defn start!
  "Start the Kafka Streams instance."
  [^KafkaStreams streams]
  (.start streams)
  streams)

(defn close!
  "Close the Kafka Streams instance.
   Optionally provide a timeout in milliseconds."
  ([^KafkaStreams streams]
   (.close streams))
  ([^KafkaStreams streams timeout-ms]
   (.close streams (Duration/ofMillis timeout-ms))))

(defn stop!
  "Stop the Kafka Streams instance. Alias for `close!`."
  ([streams] (close! streams))
  ([streams timeout-ms] (close! streams timeout-ms)))

(defn clean-up!
  "Clean up local state for the Kafka Streams instance."
  [^KafkaStreams streams]
  (.cleanUp streams))

(defn state
  "Return the current Kafka Streams state."
  [^KafkaStreams streams]
  (.state streams))

(defn query
  "Query a key-value state store by key from a running KafkaStreams instance.
   store-name must match a :store name passed to `table`, `global-table`,
   `count-values`, `reduce-values`, or `aggregate`."
  [^KafkaStreams streams store-name key]
  (let [store (.store streams
                       (StoreQueryParameters/fromNameAndType
                         store-name
                         (QueryableStoreTypes/keyValueStore)))]
    (.get store key)))