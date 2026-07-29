(ns kafka-metamorphosis.streams
  "Kafka Streams wrapper for Kafka Metamorphosis."
  (:require [kafka-metamorphosis.util :as util])
  (:import [org.apache.kafka.streams KafkaStreams StreamsBuilder Topology]
           [org.apache.kafka.streams.kstream KStream ValueMapper Predicate]
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

(defn clean-up!
  "Clean up local state for the Kafka Streams instance."
  [^KafkaStreams streams]
  (.cleanUp streams))

(defn state
  "Return the current Kafka Streams state."
  [^KafkaStreams streams]
  (.state streams))