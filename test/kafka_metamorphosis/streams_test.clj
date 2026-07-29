(ns kafka-metamorphosis.streams-test
  (:require [clojure.test :refer [deftest is testing]]
            [kafka-metamorphosis.streams :as streams])
  (:import [org.apache.kafka.streams KafkaStreams]))

(deftest test-streams-config-defaults
  (testing "default streams config"
    (let [config (streams/streams-config)]
      (is (= "kafka-metamorphosis-streams-app" (:application-id config)))
      (is (= "localhost:9092" (:bootstrap-servers config)))
      (is (= "org.apache.kafka.common.serialization.Serdes$StringSerde" (:default-key-serde config)))
      (is (= "org.apache.kafka.common.serialization.Serdes$StringSerde" (:default-value-serde config)))
      (is (= "at_least_once" (:processing-guarantee config))))))

(deftest test-builder-helpers
  (testing "stream builder helpers"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          mapped (streams/map-values stream str)
          filtered (streams/filter-stream mapped (fn [k v] (not (nil? v))))]
      (is stream)
      (is mapped)
      (is filtered))))

(deftest test-create-from-builder
  (testing "create KafkaStreams from builder"
    (let [builder (streams/create-builder)
          _ (.to (.stream builder "input-topic") "output-topic")
          topology (streams/build-topology builder)
          ks (streams/create topology (streams/streams-config "test-app"))]
      (is (instance? KafkaStreams ks))
      (is (not (nil? ks))))))
