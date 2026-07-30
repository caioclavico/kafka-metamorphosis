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

(deftest test-table-and-global-table
  (testing "table and global-table creation"
    (let [builder (streams/create-builder)
          ktable (streams/table builder "input-table-topic")
          named-table (streams/table builder "named-topic" {:store "named-store"})
          gtable (streams/global-table builder "global-topic")]
      (is ktable)
      (is named-table)
      (is gtable))))

(deftest test-map-kv-and-flat-map-kv
  (testing "map-kv and flat-map-kv transformations"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          mapped (streams/map-kv stream (fn [k v] [k (str v "!")]))
          flat-mapped (streams/flat-map-kv stream (fn [k v] [[k v] [k (str v "-dup")]]))]
      (is mapped)
      (is flat-mapped))))

(deftest test-grouping-and-windowing
  (testing "group, group-by-key and window"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          grouped (streams/group stream)
          grouped-by (streams/group-by-key stream (fn [k v] v))
          windowed (streams/window grouped 60000)]
      (is grouped)
      (is grouped-by)
      (is windowed))))

(deftest test-aggregations
  (testing "count-values, reduce-values and aggregate"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          grouped (streams/group stream)
          counted (streams/count-values grouped)
          named-counted (streams/count-values (streams/group stream) "count-store")
          reduced (streams/reduce-values grouped (fn [agg value] (str agg value)))
          aggregated (streams/aggregate grouped (constantly 0) (fn [k v agg] (+ agg 1)))]
      (is counted)
      (is named-counted)
      (is reduced)
      (is aggregated))))

(deftest test-joins
  (testing "join and left-join"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          ktable (streams/table builder "table-topic")
          joined (streams/join stream ktable (fn [v1 v2] (str v1 v2)))
          left-joined (streams/left-join stream ktable (fn [v1 v2] (str v1 v2)))]
      (is joined)
      (is left-joined))))

(deftest test-full-topology-builds
  (testing "a topology exercising the new API builds without error"
    (let [builder (streams/create-builder)
          stream (streams/stream builder "input-topic")
          ktable (streams/table builder "table-topic")
          joined (streams/join stream ktable (fn [v1 v2] (str v1 v2)))
          grouped (streams/group joined)
          counted (streams/count-values grouped "final-store")
          topology (streams/build-topology builder)]
      (is topology)
      (is (.describe topology)))))

(deftest test-create-from-builder
  (testing "create KafkaStreams from builder"
    (let [builder (streams/create-builder)
          _ (.to (.stream builder "input-topic") "output-topic")
          topology (streams/build-topology builder)
          ks (streams/create topology (streams/streams-config "test-app"))]
      (is (instance? KafkaStreams ks))
      (is (not (nil? ks))))))
